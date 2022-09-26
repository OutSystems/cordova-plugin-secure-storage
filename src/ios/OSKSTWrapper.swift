public class OSKSTWrapper: NSObject {
    private weak var delegate: OSKSTCallbackDelegate?
    
    public init(delegate: OSKSTCallbackDelegate?) {
        self.delegate = delegate
    }
}

extension OSKSTWrapper: OSKSTActionDelegate {
    public func save(service: String, account: String, data: Data, useAccessControl: Bool) {
        let query = OSKSTQuery(service: service, account: account, data: data)
        if let error = query.save(useAccessControl) {
            self.delegate?.callback(error: error)
        } else {
            self.delegate?.callbackSuccess()
        }
    }
    
    public func read(service: String, account: String? = nil) {
        let query = OSKSTQuery(service: service, account: account)
        let result = query.fetch(quantity: account != nil ? .one : .all)
        switch result {
        case .failure(let error):
            self.delegate?.callback(error: error)
        case .success(let text):
            self.delegate?.callback(text: text)
        }
    }
    
    public func delete(service: String, account: String? = nil) {
        let query = OSKSTQuery(service: service, account: account)
        let error = query.delete(quantity: account != nil ? .one : .all)
        if let error = error {
            self.delegate?.callback(error: error)
        } else {
            self.delegate?.callback(text: account ?? "")
        }
    }
}
