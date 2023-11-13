import CoreLocation
import Metal
import UIKit

import onyx

public class OnyxViewController: UIViewController {

    var device: MTLDevice!
    var metalLayer: CAMetalLayer!
	
	var timer: CADisplayLink!
	
	var pointers: Array<UITouch?> = Array(repeating: nil, count: 4)
    
    var prefix: String
    var token: String
    
    public init(prefix: String, token: String) {
        self.prefix = prefix
        self.token = token
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        onyx.shutdown()
    }
            
    public override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
		// iterate over the touches, assigning them the first available id and setting them as down
		for touch in touches {
			for (i, p) in pointers.enumerated() {
				if (p == nil) {
					pointers[i] = touch
					let location = touch.location(in: view)
					let x = (location.x / metalLayer.frame.width) * 2.0 - 1.0
					// offset by 20 to account for status bar offset
					let y = ((location.y - 20) / metalLayer.frame.height) * 2.0 - 1.0
                    onyx.setPointerPosition(UInt32(i), x, y, touch.force)
                    onyx.setPointerDown(UInt32(i))
					break;
				}
			}
		}
    }
    
    public override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
		// iterate over touches, checking their id, setting the pointer as up, and marking the id as available
		for touch in touches {
			for (i, p) in pointers.enumerated() {
				if (p == touch) {
					pointers[i] = nil
					let location = touch.location(in: view)
					let x = (location.x / metalLayer.frame.width) * 2.0 - 1.0
					// offset by 20 to account for status bar offset
					let y = ((location.y - 20) / metalLayer.frame.height) * 2.0 - 1.0
                    onyx.setPointerPosition(UInt32(i), x, y, touch.force)
                    onyx.setPointerUp(UInt32(i))
					break;
				}
			}
		}
    }
    
    public override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
		// iterate over the touches, find the id, and move the appropriate pointer
		for touch in touches {
			for (i, p) in pointers.enumerated() {
				if (p == touch) {
					let location = touch.location(in: view)
					let x = (location.x / metalLayer.frame.width) * 2.0 - 1.0
					// offset by 20 to account for status bar offset
					let y = ((location.y - 20) / metalLayer.frame.height) * 2.0 - 1.0
                    onyx.setPointerPosition(UInt32(i), x, y, touch.force)
					break;
				}
			}
		}
    }
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        
        view.isMultipleTouchEnabled = true
        device = MTLCreateSystemDefaultDevice()
        
        metalLayer = CAMetalLayer()
        metalLayer.device = device
        metalLayer.pixelFormat = .bgra8Unorm
        metalLayer.framebufferOnly = true
        metalLayer.frame = view.bounds
        metalLayer.backgroundColor = UIColor.red.cgColor
        
        view.layer.addSublayer(metalLayer)
        
        onyx.setToken(std.string(self.token))
        let unsafeMetalLayer = UnsafeMutableRawPointer(Unmanaged.passRetained(metalLayer).toOpaque())
        let unsafeMetalDevice = UnsafeMutableRawPointer(Unmanaged.passRetained(device).toOpaque())
        onyx.initialize(UInt32(metalLayer.frame.width), UInt32(metalLayer.frame.height), unsafeMetalLayer, unsafeMetalDevice, std.string(self.prefix + "/"))
		
		timer = CADisplayLink(target: self, selector: #selector(gameloop))
		timer.add(to: RunLoop.main, forMode: .default)
    }
    
    public override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        // offset by view.safeAreaInsets.top to avoid being underneath the status bar, dynamic island, notch, etc...
        metalLayer.frame = CGRect(x: 0, y: view.safeAreaInsets.top, width: view.bounds.width, height: view.bounds.height - view.safeAreaInsets.top)
    }
    
    func render() {
        onyx.render()
        metalLayer.setNeedsDisplay()
        metalLayer.display()
    }

    @objc func gameloop() {
      autoreleasepool {
        self.render()
      }
    }
    
    public func setCacheSize(_ size: Int?) {
        if let unwrapped = size {
            onyx.setCacheSize(unwrapped)
        } else {
            onyx.setCacheSize(1 << 30)
        }
    }
    
    public func purgeCache() {
        onyx.purgeCache()
    }
    
    public func point(for coordinate: CLLocationCoordinate2D) -> CGPoint? {
        let result = onyx.point(lgal.world.Vector2(coordinate.longitude, coordinate.latitude))
        return CGPoint(x: result.x, y: result.y)
    }
    
    public func coordinate(for point: CGPoint) -> CLLocationCoordinate2D? {
        let result = onyx.coordinate(lgal.world.Vector2(point.x, point.y))
        return CLLocationCoordinate2D(latitude: result.y, longitude: result.y)
    }
    
    public func loadStyle(_ url: URL) throws {
        onyx.loadStyle(std.string(url.absoluteString))
    }
    
}
