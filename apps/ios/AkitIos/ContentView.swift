import SwiftUI
import UIKit
import AkitCmp

struct ComposeContainerView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        AkitCmpEntry().mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct ContentView: View {
    var body: some View {
        ComposeContainerView()
            .ignoresSafeArea()
    }
}
