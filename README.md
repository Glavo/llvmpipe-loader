# llvmpipe-loader

A javaagent for loading [LLVMPipe](https://docs.mesa3d.org/drivers/llvmpipe.html).

It currently only supports windows x86-64. On other platforms it prints an error message instead of exiting.

Usage: 

Download the javaagent jar file from the [Release page](https://github.com/Glavo/llvmpipe-loader/releases),
then add `-javaagent:llvmpipe-loader.jar` to the JVM options.

It will extract and load the embedded `opengl32.dll`.
Because it is javaagent, the loading occurs before lwjgl is loaded, thus avoiding the system built-in opengl32.dll from being loaded,
thus forcing the use of the LLVMPipe software renderer.
