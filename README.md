# Test Onyx Package

This repository is an experimental swift package for the `onyx` viewer.
To add this is a dependency for your app, you can add it as a regular swift package.
Additionally, you will have to add cxx interoperability to your app:
* in `Project`, navigate to `Build Settings` -> `Swift Compiler`
* Under `Custom Flags` -> `Other Swift Flags` add `-cxx-interoperability-mode=default`

An example of utilizing this package can be found [here](https://github.com/nathan-stouffer-onx/InteropDemo).