//
//  Login.swift
//  Physio
//
//  Created by Yordi Agustin Paredes on 4/2/25.
//

import SwiftUI

struct Login: View {
    @State private var phoneNumber: String = ""
    
    var body: some View {
        VStack(spacing:20){
            Spacer()
            
            Text("Bienvenidos")
                .font(.largeTitle)
                .bold()
                .foregroundColor(.black)
            
            Text("Aplicación para validar ejercicios fisioterapéuticos.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundColor(.gray)
                .padding(.horizontal)
            
            Spacer()
            
            VStack(spacing: 16){
                HStack{
                    Text("¿No tienes cuenta aún?")
                        .foregroundColor(.white)
                    Button(action:{
                        
                    }){
                        Text("Registrate Ahora")
                            .bold()
                            .foregroundColor(.white)
                    }
                }
                
                TextField("Ingresa tu número de teléfono", text: $phoneNumber)
                    .padding()
                    .background(Color.white)
                    .cornerRadius(10)
                    .keyboardType(.phonePad)
                
                Button(action:{
                    
                }){
                    Text("Registrate Ahora")
                        .bold()
                        .foregroundColor(.white)
                }
                
                
            }
            .padding()
            .background(Color.purple)
            .cornerRadius(25)
            
        }
        .frame(maxWidth: .infinity)
        .background(Color.white)
        .ignoresSafeArea()
    }
}

#Preview {
    Login()
}

