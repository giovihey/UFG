cmake_minimum_required(VERSION 3.16)
project(my_project LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)        # C++17 is the sweet spot right now
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_executable(my_app src/main.cpp)