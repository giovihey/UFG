// src/main.cpp
#include <iostream>
#include <vector>
#include <string>

int main() {
    std::vector<std::string> words = {"Hello", "from", "C++!"};
    for (const auto& w : words) {
        std::cout << w << " ";
    }
    std::cout << std::endl;
    return 0;
}