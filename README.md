<h1 align="center">Bella</h1></br>

<p align="center">
:bella: A lightweight top alert, fully customizable with action and auto dismiss.
</p>
</br>
<p align="center">
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://android-arsenal.com/api?level=21"><img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat"/></a>
</p> <br>

<p align="center">
<img src="https://user-images.githubusercontent.com/8813304/94427448-60263580-018f-11eb-990f-10bbadd6f7cc.gif" width="35%"/>
</p>

## Including in your project
[![Download](https://api.bintray.com/packages/elbehiry/maven/bella/images/download.svg)](https://bintray.com/elbehiry/maven/bella/_latestVersion)
### Gradle 
Add below codes to your **root** `build.gradle` file (not your module build.gradle file).
```gradle
allprojects {
    repositories {
        jcenter()
    }
}
```
And add a dependency code to your **module**'s `build.gradle` file.
```gradle
dependencies {
    implementation "com.github.elbehiry:bella:0.1.0"
}
```

## Usage

### Basic Example for Java
Here is a basic example of implementing Bella alert  text using `Bella.Builder` class.<br>

```java
Bella bella = new Bella.Builder(context)
                .setText("you can add any content...")
                .setTextColor(ContextCompat.getColor(context, R.color.white))
                .setTextSize(15f)
                .setTextIsHtml(false)
                .setButtonText("retry")
                .setOnBellaButtonClickListener(view -> {
                    //action  here
                })
                .setPadding(6f)
                .build();
```

### Create using kotlin dsl
This is how to create `Bella` instance using kotlin dsl.

```kotlin
val bella = createBella(context){
            setText("you can add any content...")
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(15f)
            setTextIsHtml(false)
            setButtonText("retry")
            setOnBellaButtonClickListener{
                //action here
            }
            setPadding(6)
        }
```

### Show and dismiss
This is how to show bella alert view and dismiss. <br>

```kotlin
bella.showAlignedWithParent(parent: ViewGroup) // shows the bella using [ViewGroup].
```

We can dismiss popup simply using `bella.dismiss()` method.
```java
bella.dismiss()
```
We can dismiss  automatically some milliseconds later when the alert is shown using <br> 
`setAutoDismissDuration` method on `Bella.Builder`.
```java
Bella.Builder(context)
   // dismisses automatically 1000 milliseconds later when the popup is shown.
   .setAutoDismissDuration(1000L)
   ...
```
### Example Two

easy way to create bella view.
```kotlin
Bella.make(showAlert, "something went wrong", Duration.LENGTH_LONG).show()
```

### Text Composition
We can customize the text on the bella alert.

```java
.setText("You can edit your profile now!")
.setTextSize(15f)
.setTextTypeface(Typeface.BOLD)
.setTextColor(ContextCompat.getColor(context, R.color.white_87))
```

If your text has HTML in it, you can enable HTML rendering by adding this:
```java
.setTextIsHtml(true)
```

This will parse the text using `Html.fromHtml(text)`.

### TextForm
TextFrom is an attribute class that has some attributes about TextView for customizing popup text.

```java
TextForm textForm = new TextForm.Builder(context)
  .setText("This is a TextForm")
  .setTextColorResource(R.color.colorPrimary)
  .setTextSize(14f)
  .setTextTypeface(Typeface.BOLD)
  .build();

builder.setTextForm(textForm);
```

This is how to create `TextForm` using kotlin dsl.

```kotlin
val form = textForm(context) {
  text = "This is a TextForm"
  textColor = ContextCompat.getColor(context, R.color.white)
  textSize = 14f
  textTypeface = Typeface.BOLD
}
```

### OnBellaClickListener, OnBellaDismissListener, OnBellaButtonClickListener
We can listen to the bella alert button is clicked, alert is dismissed using listeners.

```kotlin
.setOnBellaClickListener { //action }
.setOnBellaDismissListener { //action }
.setOnBellaShownListener { //action }
.setOnBellaButtonClickListener { //action }
```

### Avoid Memory leak

Just use `setLifecycleOwner` method. Then `dismiss` method will be called automatically before activity or fragment would be destroyed.
```java
.setLifecycleOwner(lifecycleOwner)
```

### Lazy initialization

We should create a class which extends `Bella.Factory`.<br>
An implementation class of the factory must have a default(non-argument) constructor. <br><br>
`HomeAlertFactory.kt`
```kotlin
class HomeAlertFactory : Bella.Factory() {
    override fun create(context: Context, lifecycleOwner: LifecycleOwner): Bella {
        return Bella.Builder(context)
            .setButtonVisible(true)
            .setButtonTextColorResource(R.color.action_button_color)
            .setButtonTextSize(12f)
            .setButtonText(context.getString(R.string.retry))
            .setTextResource(R.string.alert_content)
            .setTextSize(12f)
            .setLifecycleOwner(lifecycleOwner)
            .setAutoDismissDuration(BellaViewDuration)
            .setBackgroundColorResource(R.color.error_red)
            .build()
    }
}
```

## Find this library useful? :heart:
Support it by joining __[stargazers](https://github.com/elbehiry/bella/stargazers)__ for this repository. :star:

# License
```xml
Copyright 2020 elbehiry

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```