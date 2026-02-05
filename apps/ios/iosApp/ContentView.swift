import UIKit
import SwiftUI
import AkitCmp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController() as! UIViewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        let format = NSLocalizedString("common_files", comment: "文件数量描述")
        let message = String.localizedStringWithFormat(format, 10, "folderName")
    }
}

struct ContentView: View {
    @AppStorage("akit.app.language") private var appLanguage: String = ""

    var body: some View {
        ComposeView()
            .id(appLanguage.isEmpty ? "system" : appLanguage)
            .ignoresSafeArea()
    }
}

