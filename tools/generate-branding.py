"""Export QuietDeck brand assets with Pillow. Optional argument: local TTF font path.
Original generated concept is committed; runtime uses a lightweight vector.
"""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import sys
root=Path(__file__).resolve().parents[1]
source=root/'docs/branding/quietdeck-logo-concept.png'
font_path=sys.argv[1] if len(sys.argv)>1 else 'C:/Windows/Fonts/segoeui.ttf'
brand=root/'docs/branding';store=root/'play/en-US/graphics';store.mkdir(parents=True,exist_ok=True)
with Image.open(source) as original:
    original.convert('RGBA').resize((512,512),Image.Resampling.LANCZOS).save(store/'icon.png',optimize=True)
path='M72,79H38C28,79 23,73 23,63V42C23,31 29,26 40,26H68C79,26 85,32 85,43V62C85,68 84,71 81,74 M70,77L82,89'
horizon='M27,65Q54,56 81,65'
svg=f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108"><rect width="108" height="108" rx="24" fill="#0B1423"/><g fill="none" stroke="#C6FAF5" stroke-linecap="round" stroke-linejoin="round"><path d="{path}" stroke-width="9"/><path d="{horizon}" stroke-width="2"/></g></svg>'''
(brand/'quietdeck-mark.svg').write_text(svg,encoding='utf-8')
vector=f'''<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="160dp" android:height="160dp" android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="#0B1423" android:pathData="M0,0h108v108h-108z"/><path android:fillColor="@android:color/transparent" android:strokeColor="#C6FAF5" android:strokeWidth="9" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{path}"/><path android:fillColor="@android:color/transparent" android:strokeColor="#C6FAF5" android:strokeWidth="2" android:pathData="{horizon}"/></vector>'''
(root/'app/src/main/res/drawable/ic_still.xml').write_text(vector,encoding='utf-8')
def font(n): return ImageFont.truetype(font_path,n)
def graphic(size,title_size):
    im=Image.new('RGB',size,'#0B1423');d=ImageDraw.Draw(im)
    # Original typography, derived from the brand concept; no third-party artwork.
    side=int(size[1]*.65)
    with Image.open(source) as logo: im.paste(logo.convert('RGB').resize((side,side),Image.Resampling.LANCZOS),(size[0]-side-30,(size[1]-side)//2))
    d.text((int(size[0]*.055),int(size[1]*.25)),'QuietDeck',font=font(title_size),fill='#ECFAF8')
    d.text((int(size[0]*.06),int(size[1]*.51)),'TV Launcher',font=font(int(title_size*.45)),fill='#C6FAF5')
    d.text((int(size[0]*.06),int(size[1]*.73)),'Your apps. A quieter home.',font=font(int(title_size*.28)),fill='#A4BCCB')
    return im
graphic((1024,500),78).save(store/'featureGraphic.png',optimize=True)
graphic((1280,640),96).save(brand/'social-preview.png',optimize=True)
(root/'app/src/main/res/drawable-xhdpi').mkdir(parents=True,exist_ok=True)
graphic((640,360),48).resize((320,180),Image.Resampling.LANCZOS).save(root/'app/src/main/res/drawable-xhdpi/tv_banner.png',optimize=True)
graphic((1280,720),96).save(store/'tvBanner.png',optimize=True)
print('Brand assets exported; generated concept retained separately from lightweight vector runtime icon.')
