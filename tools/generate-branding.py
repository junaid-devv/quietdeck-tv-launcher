"""Generate original project graphics. Optional developer dependency: Pillow.

Uses a local TrueType font; pass its path as the first argument on other systems.
No screenshots, third-party icons, or external artwork are used.
"""
from pathlib import Path
import sys, math
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[1]
font_path = sys.argv[1] if len(sys.argv) > 1 else 'C:/Windows/Fonts/segoeui.ttf'
def font(size): return ImageFont.truetype(font_path, size)
def mark(d, cx, cy, scale=1):
    r=42*scale
    d.ellipse((cx-r,cy-r,cx+r,cy+r),outline='#dcecf4',width=max(1,round(5*scale)))
    for delta in (14,25):d.line((cx-32*scale,cy+delta*scale,cx+32*scale,cy+delta*scale),fill='#dcecf4',width=max(1,round(4*scale)))
    d.ellipse((cx-12*scale,cy-26*scale,cx+12*scale,cy-2*scale),fill='#c0dbe7')

im=Image.new('RGB',(1280,640));d=ImageDraw.Draw(im)
for y in range(640):
    t=y/640
    c=tuple(round(a+(b-a)*t) for a,b in zip((19,44,65),(9,22,35)))
    d.line((0,y,1280,y),fill=c)
d.polygon([(780,640),(780,428),(880,371),(969,405),(1070,324),(1160,383),(1280,327),(1280,640)],fill='#233f55')
d.polygon([(730,640),(730,505),(900,456),(1000,497),(1120,432),(1280,481),(1280,640)],fill='#182f43')
mark(d,1106,186,1.35)
d.text((76,120),'QuietDeck TV',font=font(80),fill='#f2f7fa')
d.text((80,244),'A quieter home for Android TV.',font=font(35),fill='#c3d8e4')
d.text((80,325),'Minimal launcher. Your favourite apps.',font=font(25),fill='#91afc3')
for x,label,width in [(80,'Open source',162),(260,'No ads',109),(387,'Offline',112)]:
    d.rounded_rectangle((x,400,x+width,450),radius=25,fill='#284459')
    d.text((x+18,408),label,font=font(22),fill='#e0ecf3')
d.text((80,556),'github.com/junaid-devv/quietdeck-tv-launcher',font=font(22),fill='#91afc3')
im.save(root/'docs/branding/social-preview.png',optimize=True)

banner=Image.new('RGB',(640,360),'#142d42');d=ImageDraw.Draw(banner)
mark(d,320,135,1)
d.text((320,234),'QuietDeck TV',font=font(43),fill='#e8f2f7',anchor='mm')
d.text((320,290),'Your apps. A quieter home.',font=font(20),fill='#a9c3d3',anchor='mm')
banner.resize((320,180),Image.Resampling.LANCZOS).save(root/'app/src/main/res/drawable/tv_banner.png')
old=root/'app/src/main/res/drawable/tv_banner.xml'
if old.exists():old.unlink()
print('Generated original social card and TV banner.')
