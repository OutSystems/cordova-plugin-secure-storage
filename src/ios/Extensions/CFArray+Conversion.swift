extension CFArray {
    /// Converts a `CFArray` into a Swift Array.
    /// - Returns: The converted array.
    func toSwiftArray<T>() -> [T] {
        let array = [AnyObject](_immutableCocoaArray: self)
        return array.compactMap { $0 as? T }
    }
}
