//
//  Login.swift
//  Physio
//
//  Created by Yordi Agustin Paredes on 4/2/25.
//

import SwiftUI

struct Login: View {
    @State private var phoneNumber = ""
    
    var body: some View {
        ZStack {
            // Background
            LinearGradient(
                colors: [Color(red: 0.5, green: 0.4, blue: 0.8), Color(red: 0.6, green: 0.5, blue: 0.9)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Hero section
                VStack(spacing: 24) {
                    Spacer()
                    
                    // Simple illustration placeholder
                    ZStack {
                        // Character
                        VStack {
                            Circle()
                                .fill(Color(red: 0.95, green: 0.8, blue: 0.7))
                                .frame(width: 40, height: 40)
                            
                            RoundedRectangle(cornerRadius: 12)
                                .fill(.purple)
                                .frame(width: 50, height: 60)
                        }
                        
                        // Decorative elements
                        Image(systemName: "plus")
                            .foregroundColor(.white.opacity(0.6))
                            .offset(x: 60, y: -40)
                        
                        Image(systemName: "star.fill")
                            .foregroundColor(.white.opacity(0.6))
                            .font(.caption)
                            .offset(x: -50, y: 40)
                    }
                    .frame(height: 150)
                    
                    // Title and subtitle
                    VStack(spacing: 12) {
                        Text("Bienvenido")
                            .font(.custom("Poppins-Bold", size: 28))
                            .foregroundColor(.white)
                        
                        Text("Aplicación para validar\nejercicios fisioterapéuticos.")
                            .font(.custom("Poppins-Regular", size: 16))
                            .foregroundColor(.white.opacity(0.8))
                            .multilineTextAlignment(.center)
                    }
                    
                    Spacer()
                }
                .frame(maxHeight: .infinity)
                
                // Login form
                VStack(spacing: 20) {
                    // Registration link
                    VStack(spacing: 8) {
                        Text("¿No tienes cuenta aún?")
                            .font(.custom("Poppins-Regular", size: 14))
                            .foregroundColor(.gray)
                        
                        Button("Regístrate Ahora") {
                            // Registration action
                        }
                        .font(.custom("Poppins-SemiBold", size: 14))
                        .foregroundColor(.purple)
                    }
                    .padding(.top, 24)
                    
                    // Phone input
                    TextField("Ingresa tu número de teléfono", text: $phoneNumber)
                        .font(.custom("Poppins-Regular", size: 16))
                        .padding()
                        .background(Color.gray.opacity(0.1))
                        .cornerRadius(12)
                        .padding(.horizontal, 20)
                    
                    // Continue button
                    Button(action: {
                        // Continue action
                    }) {
                        Image(systemName: "arrow.right")
                            .font(.title2)
                            .foregroundColor(.white)
                            .frame(width: 60, height: 60)
                            .background(
                                phoneNumber.isEmpty ? Color.gray.opacity(0.3) : Color.purple
                            )
                            .clipShape(Circle())
                    }
                    .disabled(phoneNumber.isEmpty)
                    .padding(.bottom, 40)
                }
                .background(.white)
                .clipShape(RoundedRectangle(cornerRadius: 24))
            }
        }
    }
}

#Preview {
    Login()
}

