extension CFArray {
    func toSwiftArray<T>() -> [T] {
        let array = [AnyObject](_immutableCocoaArray: self)
        return array.compactMap { $0 as? T }
    }
}
