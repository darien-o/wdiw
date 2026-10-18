"""Draws the TV launcher banner and the app icon from the bundled fonts. Needs Pillow."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"
ITALIC = str(RES / "font/instrument_serif_italic.ttf")
INK, PAPER, AMBER = (14, 11, 9), (244, 239, 232), (255, 182, 72)


def glow(size, centre, radius, alpha):
    """A soft amber light on the ink background."""
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    x, y = centre
    ImageDraw.Draw(layer).ellipse((x - radius, y - radius, x + radius, y + radius), fill=AMBER + (alpha,))
    return layer.filter(ImageFilter.GaussianBlur(radius / 2))


def banner():
    scale = 4  # draw large, then shrink, for clean edges
    size = (320 * scale, 180 * scale)
    image = Image.new("RGBA", size, INK + (255,))
    image.alpha_composite(glow(size, (size[0] * 0.78, size[1] * 0.1), 150 * scale, 70))
    draw = ImageDraw.Draw(image)
    font = ImageFont.truetype(ITALIC, 50 * scale)
    first, second = "where do I ", "watch"
    total = draw.textlength(first + second, font=font)
    x = (size[0] - total) / 2
    y = size[1] / 2
    draw.text((x, y), first, font=font, fill=PAPER, anchor="lm")
    draw.text((x + draw.textlength(first, font=font), y), second, font=font, fill=AMBER, anchor="lm")
    out = RES / "drawable-xhdpi/banner.png"
    image.resize((320, 180), Image.LANCZOS).convert("RGB").save(out, optimize=True)
    return out


def icon():
    scale = 4
    side = 192 * scale
    image = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    mask = Image.new("L", (side, side), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, side, side), radius=44 * scale, fill=255)
    tile = Image.new("RGBA", (side, side), INK + (255,))
    tile.alpha_composite(glow((side, side), (side * 0.8, side * 0.1), 90 * scale, 90))
    draw = ImageDraw.Draw(tile)
    font = ImageFont.truetype(ITALIC, 150 * scale)
    draw.text((side * 0.38, side * 0.47), "w", font=font, fill=PAPER, anchor="mm")
    draw.text((side * 0.76, side * 0.47), "?", font=font, fill=AMBER, anchor="mm")
    image.paste(tile, (0, 0), mask)
    out = RES / "drawable-xxxhdpi/ic_launcher.png"
    image.resize((192, 192), Image.LANCZOS).save(out, optimize=True)
    return out


if __name__ == "__main__":
    for path in (banner(), icon()):
        print(path.relative_to(RES), path.stat().st_size, "bytes")
