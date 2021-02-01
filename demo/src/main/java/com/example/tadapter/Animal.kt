package com.example.tadapter

sealed class Animal


data class Dog(
    val id: Int,
    val name: String
) : Animal()

data class Cat(
    val id: Int,
    val name: String
)