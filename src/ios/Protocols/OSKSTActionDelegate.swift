public protocol OSKSTActionDelegate: AnyObject {
    func save(service: String, account: String, data: Data, useAccessControl: Bool)
    func read(service: String, account: String?)
    func delete(service: String, account: String?)
}
