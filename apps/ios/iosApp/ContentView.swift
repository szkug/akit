import UIKit
import SwiftUI
import AkitCmp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController() as! UIViewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @AppStorage("akit.app.language") private var appLanguage: String = ""

    var body: some View {
        ComposeView()
            .id(appLanguage.isEmpty ? "system" : appLanguage)
            .ignoresSafeArea()
    }
}

