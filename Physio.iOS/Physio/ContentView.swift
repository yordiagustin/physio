//
//  ContentView.swift
//  Physio
//
//  Created by Yordi Agustin Paredes on 4/2/25.
//

import SwiftUI

struct ContentView: View {
    var body: some View {
        ZStack{
            Color(UIColor.systemGroupedBackground) // Un gris claro estándar
                            .ignoresSafeArea()
            
            VStack(spacing:15) {
                Spacer()
                
                Image(systemName: "globe")
                    .imageScale(.large)
                    .foregroundStyle(.tint)
                
                Text("Physio")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .padding(.bottom, 5)
                
                Text("Aplicación para validar ejercicios fisioterapeúticos")
                    .font(.headline)
                    .fontWeight(.regular)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
                    .padding(.bottom, 40)
                
                Spacer()
            }
        }
    }
}

#Preview {
    ContentView()
}
