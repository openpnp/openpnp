## What is it?

In OpenPnP [[Computer Vision]] operations can be defined with so-called Pipelines. Authoring these Pipelines is quite a complex expert task, described on the [[CvPipeline]] page. Users normally need not author their own pipelines, they can use the built-in pipelines shipped with OpenPnP. However, because machines, cameras, lights and parts can all be different, they often need to tweak some of the _properties_ of the pipeline stages. Among the many stages and properties in a Pipeline, there are often only _very few_ essential properties that need this tweaking. But those are hard to find and often even harder to understand and adjust. It is easy to break things. 

This is where the exposed Pipeline Parameters described on this page come in. These can now be singled out by the Pipeline author and they are exposed as GUI controls on the vision settings, complete with a custom label and tooltip to explain what they do. The exposed Pipeline Parameters can therefore cleanly separate _authoring_ from _tweaking_, so that users of the pipeline need no longer understand the pipeline, its workings and how to use the Pipeline Editor. Application is now much easier.

## Application of the Parameters

Without ever going into the Pipeline Editor, users get the essential parameters presented as sliders, which they can then interactively tweak. If you don't see any parameters in your Bottom Vision settings, you probably need to [activate the new stock pipeline](/openpnp/openpnp/wiki/Computer-Vision#using-new-stock-pipelines).

![Sliders](https://user-images.githubusercontent.com/9963310/155244204-5df01b2c-02be-4475-92b4-4063c36e7acc.png)

The effect of the tweaking is directly shown in the Camera Preview. As configured in the parameter, the slider previews the stage that best shows the effect of any adjustments. In addition, if users stop dragging the slider, the pipeline end result is displayed with a slight delay. Watch it in action in the animation:

![Parametric-Pipeline](https://user-images.githubusercontent.com/9963310/155286493-f074b6b0-74c2-4d2e-ac30-3619a70f424d.GIF)

## Configuration of the Pipeline (Advanced Users)

### Parametric Control of a Stage 

In our example we want to control a `MaskHsv` stage named "threshold", and in it the "valueMax" property: 

![target stage](https://user-images.githubusercontent.com/9963310/155367133-c5e76f86-1995-4084-a81e-70918c521a04.png)

The following will show you how to achieve that.

### Create New ParameterNumeric Stage

Create a new `ParameterNumeric` stage by pressing the ![ + ](https://user-images.githubusercontent.com/9963310/155365599-865f0a2c-81db-4401-932e-a49ddb75b950.png) button. Type "para" to jump to the right stage implementation:

![New Stage](https://user-images.githubusercontent.com/9963310/155365184-8a9a6a0c-fdab-43ac-9fe0-33df1dd8f3cd.png)

Then drag & drop the new stage to the top of the list. Parameter stages can only influence the stages that come _after_ them, and it is also a well known convention to have the parameters at the top.

![Stage Name](https://user-images.githubusercontent.com/9963310/155367974-aaf7af6d-f920-434a-926f-a46f3798b3e5.png)

Change the new stage's **Name** in the Name column of the list. This name also sets the parameter name, i.e. the key under which the setting is stored in the vision settings. It is important that you choose a well-considered, stable name. If you later change that name, any stored parameter assignments will be disconnected. The **Name** must not be numeric, i.e. the default number that is assigned by the Pipeline Editor when a stage is created, is not allowed. The Parameter will remain inactive until you assign a proper **Name**. An exception is reported in the stage, as long as this is not yet the case. 

### ParameterNumeric Properties

The `ParameterNumeric` stage has the following properties:

![ParameterNumeric Properties](https://user-images.githubusercontent.com/9963310/155368795-c6cd41f0-e059-4153-92f7-ad124a0359d4.png)

The **parameterLabel** determines the label of the parameter, when it is added to the user interface. Unlike the stage name, the label may be changed anytime, as you like.

The **parameterDescription** determines the tool-tip of the parameter, when it is added to the user interface. You can use HTML markup, if you like (due to limitations of the property editor, editing is restricted to one line).

The **stageName** identifies the target stage that is controlled by this parameter. It is recommended to properly name the target stage and then copy the name over here. **Note:** The identified target stage must come _after_ this parameter stage.

The **propertyName** identifies the property in the target stage. The identified property must be of numeric type (hence our stage is called "ParameterNumeric"). Future development may cover additional property types.  

The **numericType** sets the type and unit of the target property. Even though these properties are "all just numbers", there are some nuances to be distinguished:

![image](https://user-images.githubusercontent.com/9963310/155369187-7d4281c3-1013-455a-a6c1-c078f4adf90d.png)

- **Integer** controls the property as a whole number. It is typically used when color channels values (0 ... 255) are addressed, like in our example.
- **Double** controls the property as a plain floating point number.
- **Squared** controls the property on a squared scale, i.e. the linear scale of the slider is squared before being applied to the property. Effectively you can control a 2D area by its 1D dimension, which gives the slider a more natural control (before using this type, read about the **SquareMillimeters** and **SquareMillimetersToPixels** types below).
- **Exponential** controls the property on an exponential scale, i.e. the linear scale of the slider controls the _magnitude_ (exponent) of the number to be applied to the property. Use this if the property goes over a very large range. You can then control the small numbers finely, and the large numbers coarsely. 
- **Millimeters** controls dimensional properties that are given as absolute lengths/millimeters. Note: the parameter is always defined in millimeters, but the user interface will still convert everything to inches, and back, if that is the user's system preference. But the pipeline remains portable across systems that way. 
- **MillimetersToPixels** controls dimensional properties given in pixels, but the parameter is handled and stored as a length with units. This makes the parameter independent of the resolution, distance and viewing angle of the camera. It will automatically be converted to pixels using the proper **Units per Pixel** scale of the camera, and it can even adjust for the Z distance of the subject, [if 3D calibration is enabled](https://github.com/openpnp/openpnp/wiki/3D-Units-per-Pixel). 
- **SquareMillimeters** controls area properties that are given as areas/square millimeters. It is the squared variant of the **Millimeters** type, i.e. what was said about **Squared** and **Millimeters** applies in combination. 
- **SquareMillimetersToPixels** controls area properties given in pixels, but the parameter is handled and stored as an area with units.  It is the squared variant of the **MillimetersToPixel** type, i.e. what was said about **Squared** and **MillimetersToPixel** applies in combination. 

The **minimumValue** and **maximumValue** control the limits of the parameter range. If  **minimumValue** is larger than **maximumValue**, the slider control is effectively reversed, which may be more intuitive for certain parameters. The values are given as millimeters, or square millimeters if one of the corresponding **numericType**s is selected. Any squared or exponential scaling does only affect how the slider interpolates _between_ minimum and maximum, i.e. you do not need to do any math yourself.

The **defaultValue** is the preset for the controlled property, when the pipeline is first applied.  Again, the value is given as millimeters, or square millimeters if one of the corresponding **numericType**s is selected.

The **effectStageName** identifies the stage in the pipeline, where the effect of changes to the controlled property is best seen by the user. When users move the slider, the preview will dynamically show the effect stage image. This is often the same stage as the target stage given by the **stageName** (see above), but there are cases where the property only controls an _input_ that needs further processing in subsequent stages, before it shows up as a visible effect. If **effectStageName** is left empty, no effect preview is provided. 

The **previewResult** checkbox determines, if the result image of the pipeline is previewed, similar to the effect stage. If both  **effectStageName** and **previewResult** are given, the result image preview will only be shown after a delay, when the user stopped dragging the slider.  
