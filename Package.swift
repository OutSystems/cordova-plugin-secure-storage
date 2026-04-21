// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "cordova-plugin-secure-storage",
    products: [
        .library(
            name: "cordova-plugin-secure-storage",
            targets: ["cordova-plugin-secure-storage"]
        )
    ],
    targets: [
        .binaryTarget(
            name: "OSKeyStoreLib",
            path: "src/ios/frameworks/OSKeyStoreLib.xcframework"
        ),
        .target(
            name: "cordova-plugin-secure-storage",
            dependencies: ["OSKeyStoreLib"],
            path: "src/ios",
            sources: ["SecureStorage.swift"]
        )
    ]
)
