"""R17: author the USER-APPROVED generated design, using its front-view landmarks.

Design-plane coordinates below refer to the left panel of the 1536x1024 sheet.
No raster is used as a runtime texture. Every shell, aperture and tendon is 3D.
The drawing's elbow is (335,280); scale is 0.0018 model metres per design pixel.
"""
import math
from build_reference_weapons import Model, surface, sample, cord, plate, BLACK

SCALE=.0018
SHELL=(57,59,63); FACE=(78,80,83); RIDGE=(103,106,109)
BEVEL=(137,141,143); EDGE=(177,181,182)
FLESH=(76,32,34); FIBRE=(101,44,45); DARK=(31,28,31)

def xy(u,v):return ((u-335)*SCALE,(v-280)*SCALE)

def ribbon(points,colors=None,closed=False):
    """Swept solid blade/shell strip: u,v,depth,half width px,half thickness m."""
    rows=sample(points,4);rings=[]
    for i,(u,v,z,w,h) in enumerate(rows):
        a=rows[max(0,i-1)];b=rows[min(len(rows)-1,i+1)]
        dx,dy=b[0]-a[0],b[1]-a[1];length=max(1e-8,math.hypot(dx,dy))
        px,py=dy/length,-dx/length;x,y=xy(u,v);w=max(w*SCALE,.0005)
        profile=[(-1,0),(-.86,-.32),(-.13,-1),(.82,-.28),(1,0),(.82,.28),(-.13,1),(-.86,.32)]
        rings.append([(x+px*w*l,y+py*w*l,z+h*d) for l,d in profile])
    return surface(rings,colors or [BEVEL,SHELL,FACE,BEVEL,BEVEL,FACE,SHELL,BEVEL],'crease',not closed)

def blade_face(rows):
    """Explicit outer/inner edge silhouettes: v, left u, right u, thickness."""
    rings=[]
    for v,l,r,h in sample(rows,4):
        x,y=xy(l,v);width=(r-l)*SCALE;bevel=min(.018,width*.14)
        ridge=x+width*.62
        rings.append([(x,y,0),(x+bevel,y,-h*.25),(ridge,y,-h),
                      (x+width-.006,y,-h*.18),(x+width,y,0),
                      (x+width-.006,y,h*.18),(ridge,y,h),(x+bevel,y,h*.25)])
    return surface(rings,[EDGE,FACE,SHELL,RIDGE,RIDGE,SHELL,FACE,BEVEL],'crease')

def tendon(points,color=FLESH):
    # Stable path-normal cross sections allow diagonal and branching tissue.
    rows=sample(points,3);rings=[]
    for i,(u,v,z,w,h) in enumerate(rows):
        a=rows[max(0,i-1)];b=rows[min(len(rows)-1,i+1)]
        dx,dy=b[0]-a[0],b[1]-a[1];length=max(1e-8,math.hypot(dx,dy))
        px,py=dy/length,-dx/length;x,y=xy(u,v)
        rings.append([(x+px*w*SCALE*math.cos(t),y+py*w*SCALE*math.cos(t),z+h*math.sin(t)) for t in [j*math.tau/8 for j in range(8)]])
    return surface(rings,[color])

def aperture(cx,cy,rx,ry,tilt,width):
    # A real through-hole: no cap or dark disk fills the inside of this rim.
    rows=[];angle=math.radians(tilt)
    for i in range(33):
        t=math.tau*i/32;x=rx*math.cos(t);y=ry*math.sin(t)
        rows.append((cx+x*math.cos(angle)-y*math.sin(angle),cy+x*math.sin(angle)+y*math.cos(angle),0,width*(1+.15*math.cos(t*3)),.024+.010*math.cos(t)**2))
    return ribbon(rows,[RIDGE,SHELL,FACE,BEVEL,BEVEL,FACE,SHELL,RIDGE],True)

def build():
    m=Model()
    # Compact proximal tissue: irregular layered armour, no bulbous cuff.
    m.add('Arm_R','R / upper tissue',cord([(0,-.03,0,.105,.112),(-.008,.09,0,.122,.116),(-.013,.20,0,.110,.101),(0,.33,0,.088,.085)],DARK,12,.08))
    for side in [-1,1]:
        for i,x in enumerate([-.065,.014,.072]):
            m.add('Arm_R',f'R / upper layered shell {side} {i}',plate([(x*.9,-.025+i*.025,side*.078,.002,.002),(x*1.3,.09+i*.02,side*.111,.024,.016),(x*.85,.205,side*.094,.020,.013),(x*.3,.33,side*.068,.005,.005)]))
    # Forearm changes into branches BEFORE the first aperture, leaving it open.
    m.add('Hand_R','R / elbow tissue',cord([(0,-.035,0,.078,.073),(-.016,.048,0,.083,.072),(-.035,.112,0,.064,.057),(-.067,.185,0,.025,.024)],DARK,12,.06))

    # User correction: both cutting ends belong to ONE continuous blade shell.
    # Every adjacent ring shares its full seam; there is no gap bridged by rods,
    # tissue or a separate crescent. Apertures live inside the adjacent root web.
    m.add('Hand_R','R / continuous double-ended blade',blade_face([
        (50,299,300,.0007),(85,275,285,.008),(126,249,269,.014),
        (175,228,253,.021),(225,209,240,.026),(267,195,230,.027),
        (300,185,231,.030),(329,180,230,.026),(349,199,247,.036),
        (369,187,258,.041),(394,197,257,.044),(419,208,263,.043),
        (439,213,272,.042),(451,197,277,.041),(469,199,289,.045),
        (492,205,302,.047),(525,213,309,.046),(570,226,319,.043),
        (630,245,328,.037),(695,269,333,.029),(758,295,337,.021),
        (818,323,343,.012),(866,345,352,.005),(895,356,357,.0007)]))
    # Connect the reverse blade's base through a substantial fork, not thin rods.
    m.add('Hand_R','R / reverse blade fork',ribbon([
        (225,260,0,4,.015),(231,290,0,13,.038),(249,310,0,20,.047),
        (261,337,0,18,.050),(238,367,0,13,.041),(199,396,0,1,.002)]))
    m.add('Hand_R','R / large oval rim',aperture(319,407,21,33,-12,7))
    m.add('Hand_R','R / small oval rim',aperture(320,495,12,22,24,5))

    # Main tissue routes stay to the sides of the two voids. The hard shell
    # rides on these roots, rather than disconnected jewellery over a tube.
    routes=[
        [(354,239,9),(345,293,13),(321,330,12),(293,363,12),(284,416,10),(295,458,8),(305,478,5)],
        [(377,245,7),(363,288,12),(356,338,10),(349,386,7),(347,433,8),(336,458,6),(321,466,3)],
        [(294,307,9),(270,335,12),(255,382,12),(245,421,10),(239,454,8),(263,479,6),(280,519,2)],
        [(303,452,6),(289,489,8),(301,534,8),(319,578,6),(327,644,3),(331,699,1)],
    ]
    def tissue_depth(v):return max(.025,min(1,(770-v)/250))
    for side in [-1,1]:
        for i,route in enumerate(routes):
            m.add('Hand_R',f'R / fused tissue route {side} {i}',tendon([(u,v,side*.017*tissue_depth(v),w,(.027 if j not in [0,len(route)-1] else .012)*tissue_depth(v)) for j,(u,v,w) in enumerate(route)],DARK))
            for strand in range(3):
                shift=(strand-1)*4
                m.add('Hand_R',f'R / red muscle fibre {side} {i} {strand}',tendon([(u+shift,v+strand*3,side*(.042+(strand==1)*.006)*tissue_depth(v),max(.4,w*.23),(.004+(w/12)*.005)*tissue_depth(v)) for u,v,w in route],FLESH if strand!=1 else FIBRE))
        # One dominant diagonal root, plus asymmetric sweeping plates. Avoid
        # the identical crossed ribbons that made the rejected R16 look braided.
        shells=[
            [(365,212,1),(348,259,9),(321,290,12),(287,321,13),(250,349,12),(214,381,6),(198,401,1)],
            [(379,223,1),(367,272,9),(348,310,12),(331,342,10),(322,371,5)],
            [(285,314,1),(272,343,11),(250,378,10),(239,413,7),(230,444,1)],
            [(351,324,1),(351,349,9),(363,365,1),(345,377,8),(343,420,7),(351,439,1)],
            [(294,427,1),(294,450,8),(314,465,8),(338,459,6),(354,445,1)],
            [(346,469,1),(340,493,7),(337,515,5),(345,521,1),(329,535,6),(321,565,3)],
            [(296,518,1),(300,549,7),(319,591,6),(327,655,3),(334,785,1)],
            [(211,478,1),(231,515,5),(255,575,5),(279,666,3),(326,806,1)],
        ]
        for i,path in enumerate(shells):
            m.add('Hand_R',f'R / overlapping carapace {side} {i}',ribbon([(u,v,side*(.105 if v<335 else .072 if v<450 else .046*max(.02,min(1,(895-v)/365))),w,.016 if w>2 else .002) for u,v,w in path]))
    # Rear/outside of the forearm: keep the reverse edge behind the elbow.
    mount=-45;a=math.radians(mount);c,s=math.cos(a),math.sin(a)
    for faces in m.joints.values():
        for face in faces:
            face['p']=[[round(c*x+s*z,6),y,round(-s*x+c*z,6)] for x,y,z in face['p']]
            if 'n' in face:face['n']=[[round(c*x+s*z,6),y,round(-s*x+c*z,6)] for x,y,z in face['n']]
    m.save('form_blade',revision=18,mount_degrees=mount)

if __name__=='__main__':build()
