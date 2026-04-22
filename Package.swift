// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "cordova-plugin-secure-storage",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "cordova-plugin-secure-storage",
            targets: ["cordova-plugin-secure-storage"])
    ],
    dependencies: [
        .package(url: "https://github.com/apache/cordova-ios.git", branch: "master")
    ],
    targets: [
        .binaryTarget(
            name: "OSKeyStoreLib",
            path: "src/ios/frameworks/OSKeyStoreLib.xcframework"
        ),
        .target(
            name: "cordova-plugin-secure-storage",
            dependencies: [
                .product(name: "Cordova", package: "cordova-ios"),
                .target(name: "OSKeyStoreLib")
            ],
            path: "src/ios",
            exclude: [
                "frameworks/OSKeyStoreLib.xcframework"
            ])
    ]
)