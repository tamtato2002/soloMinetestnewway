from PIL import Image
import json

PAL = {
 '.': None,
 'k': (0x1b,0x1b,0x1b), 'w': (0xff,0xff,0xff), 'g': (0xc6,0xc6,0xc6), 'G': (0x7b,0x7b,0x7b), 'D': (0x4a,0x4a,0x4a),
 'b': (0x9c,0x6a,0x3a), 'B': (0x5e,0x3b,0x1e), 'r': (0xe0,0x3c,0x3c), 'R': (0x8f,0x1c,0x1c), 'q': (0xff,0x8a,0x8a),
 'o': (0xf0,0x93,0x2a), 'y': (0xfc,0xe4,0x4a), 'Y': (0xc9,0x9a,0x1a),
 'c': (0x5c,0xe0,0xe0), 'C': (0x2a,0x7f,0xd0), 'n': (0x16,0x3a,0x8a),
 'l': (0x7c,0xe0,0x4a), 'L': (0x2f,0x8f,0x2a),
 'p': (0xb0,0x6c,0xe8), 'P': (0x5a,0x2a,0x9a),
 's': (0xe0,0xb0,0x8a), 'S': (0xb0,0x7c,0x56), 'h': (0x4a,0x2c,0x14),
 'W': (0xee,0xe6,0xd2), 'V': (0xc9,0xbd,0x9a),
}

class G:
    def __init__(s): s.g=[['.']*16 for _ in range(16)]
    def set(s,x,y,c):
        if 0<=x<16 and 0<=y<16: s.g[y][x]=c
    def rect(s,x,y,w,h,c):
        for j in range(h):
            for i in range(w): s.set(x+i,y+j,c)
    def line(s,x0,y0,x1,y1,c):
        dx=abs(x1-x0); dy=abs(y1-y0); sx=1 if x0<x1 else -1; sy=1 if y0<y1 else -1; e=dx-dy
        while True:
            s.set(x0,y0,c)
            if x0==x1 and y0==y1: break
            e2=2*e
            if e2>-dy: e-=dy; x0+=sx
            if e2<dx: e+=dx; y0+=sy
    def pts(s,c,*p):
        for x,y in p: s.set(x,y,c)
    def outline(s):
        out=[r[:] for r in s.g]
        for y in range(16):
            for x in range(16):
                if s.g[y][x]=='.':
                    for dx,dy in((1,0),(-1,0),(0,1),(0,-1)):
                        nx,ny=x+dx,y+dy
                        if 0<=nx<16 and 0<=ny<16 and s.g[ny][nx] not in '.k':
                            out[y][x]='k'; break
        s.g=out
    def rows(s): return [''.join(r) for r in s.g]

def sword():
    g=G()
    # blade: diagonal, 2px wide, light edge on top
    for i in range(9):
        g.set(5+i,10-i,'g'); g.set(6+i,10-i,'G')
        g.set(5+i,9-i,'w') if i>0 else None
    g.set(13,1,'w'); g.set(14,1,'g'); g.set(14,2,'G')
    # guard (perpendicular)
    g.line(3,8,7,12,'Y'); g.line(4,8,8,12,'o')
    # grip + pommel
    g.line(3,11,2,12,'b'); g.line(2,12,1,13,'B'); g.set(1,14,'Y'); g.set(0,14,'Y'); g.set(1,15,'Y')
    g.outline(); return g

def head():
    g=G()
    g.rect(2,2,12,12,'s')
    g.rect(2,2,12,4,'h')            # hair
    g.rect(2,6,2,2,'h'); g.rect(12,6,2,2,'h')
    g.rect(4,7,3,2,'w'); g.rect(9,7,3,2,'w')   # eyes
    g.rect(5,7,2,2,'C'); g.rect(9,7,2,2,'C')
    g.rect(7,9,2,2,'S')             # nose
    g.rect(5,11,6,1,'R'); g.rect(6,11,4,1,'R')  # mouth
    g.rect(2,13,12,1,'S')
    g.rect(3,5,2,1,'h'); g.rect(11,5,2,1,'h')
    g.outline(); return g

def book():
    g=G()
    g.rect(3,1,10,14,'P'); g.rect(3,1,10,1,'p'); g.rect(3,1,1,14,'p')
    g.rect(4,2,8,12,'P')
    g.rect(2,2,2,12,'n')            # spine
    g.rect(12,2,1,12,'p')
    # pages edge
    g.rect(4,14,9,1,'W')
    # glowing rune / star
    g.pts('y',(8,5),(7,6),(8,6),(9,6),(6,7),(7,7),(8,7),(9,7),(10,7),(7,8),(8,8),(9,8),(8,9))
    g.pts('w',(8,6),(8,7))
    g.pts('c',(5,4),(11,10),(5,11))
    g.outline(); return g

def paper():
    g=G()
    g.rect(2,1,11,14,'W'); g.rect(2,1,11,1,'w'); g.rect(2,1,1,14,'w')
    g.rect(12,2,1,13,'V'); g.rect(3,14,10,1,'V')
    # folded corner
    g.pts('.',(11,1),(12,1),(12,2)); g.pts('V',(11,2),(11,3),(12,3),(10,2)); 
    # text lines
    for y in (5,7,9):
        g.rect(4,y,6,1,'G')
    # green check
    g.pts('L',(5,12),(6,13),(7,12),(8,11),(9,10),(10,9))
    g.pts('l',(5,11),(6,12),(7,11),(8,10),(9,9))
    g.outline(); return g

def shield():
    g=G()
    g.rect(2,1,12,9,'C')
    for i,(x,w) in enumerate([(2,12),(3,10),(4,8),(5,6),(6,4)]):
        g.rect(x,10+i,w,1,'C')
    g.rect(2,1,12,1,'c'); g.rect(2,1,1,9,'c')
    # iron rim
    g.rect(2,1,12,1,'g'); g.rect(2,1,1,9,'g'); g.rect(13,1,1,9,'G')
    g.pts('g',(3,10),(4,11),(5,12),(6,13)); g.pts('G',(12,10),(11,11),(10,12),(9,13))
    # cross
    g.rect(7,2,2,11,'y'); g.rect(3,5,10,2,'y'); g.rect(7,2,2,1,'Y'); g.rect(7,12,2,1,'Y')
    g.pts('o',(8,3),(8,4),(8,8),(8,9),(8,10),(8,11),(9,6))
    g.outline(); return g

def cog():
    g=G()
    import math
    cx=cy=7.5
    for y in range(16):
        for x in range(16):
            dx,dy=x-cx,y-cy; r=math.hypot(dx,dy)
            ang=math.atan2(dy,dx)
            tooth = 7.2 if (int(round(ang/(math.pi/4)))%2==0) else 5.4
            # 8 teeth: radius bigger near tooth centres
            a=(ang/(math.pi/4))%1
            rad = 7.0 if (a<0.28 or a>0.72) else 5.3
            if r<=rad and r>=2.3: g.set(x,y,'g')
    # shading
    for y in range(16):
        for x in range(16):
            if g.g[y][x]=='g':
                if (x-cx)+(y-cy) > 3.5: g.g[y][x]='G'
                elif (x-cx)+(y-cy) < -6: g.g[y][x]='w'
    # inner ring darker
    for y in range(16):
        for x in range(16):
            r=((x-cx)**2+(y-cy)**2)**.5
            if g.g[y][x]!='.' and r<3.6: g.g[y][x]='D'
    g.outline(); return g

def heart():
    g=G()
    rows=[
     "................",
     "..rrrr....rrrr..",
     ".rqqrrr..rrrrrr.",
     ".rqrrrrrrrrrrrR.",
     "rrrrrrrrrrrrrrRR",
     "rrrrrrrrrrrrrrRR",
     "rrrrrrrrrrrrrrRR",
     ".rrrrrrrrrrrrRR.",
     ".rrrrrrrrrrrrRR.",
     "..rrrrrrrrrrRR..",
     "...rrrrrrrrRR...",
     "....rrrrrrRR....",
     ".....rrrrRR.....",
     "......rrRR......",
     ".......RR.......",
     "................"]
    for y,r in enumerate(rows):
        for x,c in enumerate(r): g.set(x,y,c)
    g.outline(); return g

def feather():
    g=G()
    widths=[0,1,2,2,3,3,2,2,1,0]
    for i,w in enumerate(widths):
        x,y=3+i,12-i
        for t in range(-w,w+1):
            c='w' if t<0 else ('W' if t==0 else 'V')
            g.set(x+t,y+t,c); g.set(x+t+1,y+t,c)
    g.line(3,12,12,3,'V')                      # center vein
    g.line(1,14,3,12,'b'); g.set(0,15,'B')    # quill shaft
    g.pts('c',(6,7),(8,5),(9,6))
    g.outline(); return g

def lapis():
    g=G()
    rows=[
     "................",
     "................",
     "....nnnnnn......",
     "...nCCcCCCCn....",
     "..nCcwcCCnCCn...",
     "..nCcCCCCCCCn...",
     ".nCCCCnCCcCCCn..",
     ".nCCnCCCCCCCCn..",
     ".nCCCCCcCCCnCn..",
     "..nCCcCCCCCCCn..",
     "..nCCCCnCCCCn...",
     "...nCCCCCCCCn...",
     "....nnCCCCnn....",
     "......nnnn......",
     "................",
     "................"]
    for y,r in enumerate(rows):
        for x,c in enumerate(r): g.set(x,y,c)
    g.outline(); return g

def eye():
    g=G()
    # eye of ender: green orb with pupil
    import math
    cx=cy=7.5
    for y in range(16):
        for x in range(16):
            r=math.hypot(x-cx,y-cy)
            if r<=6.6: g.set(x,y,'L')
            if r<=5.6: g.set(x,y,'l')
            if r<=5.6 and (x-cx)+(y-cy)>3: g.set(x,y,'L')
    g.rect(5,5,6,6,'k'); g.rect(6,6,4,4,'G'); g.rect(7,7,2,2,'y')
    g.pts('w',(4,4),(5,3),(3,5))
    g.pts('c',(11,4),(4,11))
    g.outline(); return g

ICONS = {'status':head,'skills':book,'quests':paper,'resist':shield,'config':cog,
         'str':sword,'vit':heart,'agi':feather,'int':lapis,'per':eye}

if __name__=='__main__':
    names=list(ICONS)
    S=10; pad=2
    W=len(names)*(16+pad)*S; H=(16+pad)*S
    im=Image.new('RGB',(W,H),(0x3a,0x3a,0x44))
    for n,name in enumerate(names):
        g=ICONS[name]()
        for y,row in enumerate(g.rows()):
            for x,c in enumerate(row):
                col=PAL[c]
                if col:
                    for j in range(S):
                        for i in range(S):
                            im.putpixel((n*(16+pad)*S+pad*S//2+x*S+i, pad*S//2+y*S+j), col)
    im.save('preview.png')
    json.dump({n:ICONS[n]().rows() for n in names}, open('icons.json','w'))
    print('ok')
