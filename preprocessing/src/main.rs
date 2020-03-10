extern crate lapp;
extern crate shapefile;

use shapefile::*;

fn main() {
    let args = lapp::parse_args("
    Preprocess shapefiles into more efficient files.
      <file> (string) input file name"
    );

    let file = args.get_string("file");

    println!("Shapefile processor...");
    if let Ok(shapes) = shapefile::read(file.clone()){
        println!("Shapes: {}", shapes.len());
        compressHeightmap(shapes);
    }else{
        println!("Could not read file: {}", file);
    }
}

type P2 = (f64,f64);
type P3 = (f64,f64,f64);

struct ShapeZ{
    points: Vec<P2>,
    z: f64,
    bb: (P3,P3),
}

fn compressHeightmap(shapes: Vec<Shape>) -> Vec<ShapeZ>{
    let mut shapezs = Vec::new();
    'outer: for shape in shapes{
        match shape {
            Shape::PolylineZ(polylineZ) => {
                if polylineZ.parts.len() > 1{
                    println!("Warning: skipped shape, more than 1 part!");
                    continue;
                }
                if polylineZ.points.is_empty(){
                    println!("Warning: skipped shape, 0 points!");
                    continue;
                }
                let mut npoints = Vec::new();
                let z = polylineZ.points[0].z;
                let mut minx = std::f64::MAX;
                let mut maxx = std::f64::MIN;
                let mut miny = std::f64::MAX;
                let mut maxy = std::f64::MIN;
                let mut minz = std::f64::MAX;
                let mut maxz = std::f64::MIN;
                for point in polylineZ.points {
                    if point.z != z{
                        println!("Warning: skipped shape, not all z equal!");
                        continue 'outer;
                    }
                    minx = minx.min(point.x);
                    maxx = maxx.max(point.x);
                    miny = miny.min(point.y);
                    maxy = maxy.max(point.y);
                    minz = minz.min(point.z);
                    maxz = maxz.max(point.z);
                    npoints.push((point.x,point.y));
                }
                let bb = ((minx,miny,minz),(maxx,maxy,maxz));
                shapezs.push(ShapeZ{
                    points: npoints,
                    z,
                    bb,
                });
            },
            _ => { println!("Bruh"); }
        }
    }
    shapezs
}

