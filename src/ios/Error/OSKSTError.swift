/// All plugin errors that can be thrown
public enum OSKSTError: Int, CustomNSError, LocalizedError {
    case badArguments = 1
    case operationUnimplemented = 2
    case invalidParameters = 3
    case memoryAllocation = 4
    case unavailable = 5
    case duplicateItem = 6
    case itemNotFound = 7
    case userInterationNotAllowed = 8
    case dataDecode = 9
    case authenticationFailed = 10
    case general = 11
    
    /// Textual description
    public var errorDescription: String? {
        switch self {
        case .badArguments:
            return "Some of the arguments are not valid."
        case .operationUnimplemented:
            return "Function or operation not implemented."
        case .invalidParameters:
            return "One or more parameters passed to a function are not valid."
        case .memoryAllocation:
            return "Failed to allocate memory."
        case .unavailable:
            return "No Keychain is available. A restart may be needed."
        case .duplicateItem:
            return "The specified item already exists in the Keychain."
        case .itemNotFound:
            return "The specified item could not be found in the Keychain."
        case .userInterationNotAllowed:
            return "User interaction is currently not allowed."
        case .dataDecode:
            return "Unable to decode the provided data."
        case .authenticationFailed:
            return "The user name or passphrase you entered is not correct."
        default:
            return "An error just occurred."
        }
    }
}
