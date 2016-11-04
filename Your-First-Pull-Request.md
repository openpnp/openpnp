# Your First Pull Request

A step by step tutorial to help new contributors submit their first pull request. By the end of this tutorial you will have a basic understanding of Git, Github and Pull Requests.

# Why Pull Requests?

OpenPnP, and many other Open Source projects maintain their source code and project files in a Git repository. Git is a version control system which allows people to keep track of changes to files over time. The benefit of using a version control system is that you can go back in time and see every change that has ever been made to a project. This is very helpful when trying to find bugs.

A Pull Request is the Git way of asking the maintainer of a project to accept some changes you'd like to make. In most projects there is a small team of maintainers who do development and accept changes from other people. They make sure the changes fit into the architecture of the program and help to catch problems before they are distributed to users.

# A Bit About Github

Github is the most popular Git hosting platform in the world. At it's heart it is a way to manage one or more Git repositories but it also adds very useful things like issue tracking, wiki Pages, collaboration and web based code viewing. Git and Github are separate things. Git is the version control system and Github is a very nice front end for using Git.

Before we get started, you'll need a Github account. It's free and only takes a second to sign up. Once you have one you can contribute to OpenPnP, create your own repositories, file issues and bug reports, create wiki pages and all kinds of stuff.

You can create a Github account at https://github.com/join. You should do that before continuing.

# What's Going to Happen?

In this tutorial we are going to make a copy of the OpenPnP repository, make a small change to a file, commit the change and submit a pull request asking the maintainers of OpenPnP to accept the change. It sounds complex, but it's really easy, and Github makes it even easier!

# Getting Started

1. To get started, make sure you have created your [Github account](https://github.com/join) and signed in.
2. You'll need to install the Git command line tools. You don't always have to use the command line, and in fact I recommend you don't, but this tutorial uses command lines because they are compatible with every operating system. You can learn how to install Git at https://git-scm.com/book/en/v1/Getting-Started-Installing-Git.
3. Open up a new tab in your browser to https://github.com/openpnp/openpnp. You'll want to be able to do work in that tab while you keep this one open so you don't lose your place.

# Fork It!

The first thing we'll do is fork the OpenPnP repository. Forking just means making a copy of the entire repository. Github will copy the OpenPnP repository from the OpenPnP account to your account. This gives you your own copy you can do whatever you want with. Don't worry, Github doesn't care how much space you use. You can fork as many repositories as you like.

Tip: We're going to start saying "repo" instead of repository because I am sick of typing repository, and repo is what everyone says anyway.

To fork the repo, go to the [OpenPnP](https://github.com/openpnp/openpnp) repo in your other browser tab and click the Fork button in the upper right hand corner. Github might ask you where you want to put it and you can just use your account.

![screen shot 2016-11-04 at 12 06 57 am](https://cloud.githubusercontent.com/assets/1182323/19995185/fca56a28-a222-11e6-9bed-87458100683e.png)

Once you click Fork, Github will work it's magic...

![screen shot 2016-11-04 at 12 10 08 am](https://cloud.githubusercontent.com/assets/1182323/19995197/236c9abe-a223-11e6-9be6-bf5d45c36f02.png)

And when it's done it will redirect you to your own copy of the OpenPnP repo!

![screen shot 2016-11-04 at 12 11 11 am](https://cloud.githubusercontent.com/assets/1182323/19995219/39ec29ee-a223-11e6-9d86-e44639c30976.png)

Notice that before we forked the repo we were looking at the `openpnp/openpnp` repo and now we're looking at `vonnieda/openpnp`. The first one is the main OpenPnP repo and it's owned by the openpnp organization. The new one is owned by you (or in the screenshot, me) so it shows up under vonnieda. That's my username. Yours will be your username.

# Clone It!

Now that you have your own copy of the OpenPnP repo you can copy it to your computer so you can start making changes. In Git land this is called Cloning. Cloning is similar to Forking, and behind the scenes that's actually what Github does when you Fork a repo but to keep things straight we'll use the terminology that Github uses.

To clone the repo, look for the Clone or Download button on the repo page and click it. You'll see an icon that will let you copy the URL of your repo, and you should click that.

![screen shot 2016-11-04 at 12 16 09 am](https://cloud.githubusercontent.com/assets/1182323/19995290/ea20b7da-a223-11e6-86aa-5971f7d26b9b.png)

Tip: Ignore the blue button that says Download Zip. You want the little clipboard button next to the URL.

Important Tip: Make sure you are looking at YOUR fork of the repo and not the OpenPnP one. Remember, the name up top should be `your_username/openpnp`, not `openpnp/openpnp`. If you've ended up back on the OpenPnP one, just click your avatar in the upper right corner and then click Your Profile. You'll find your fork of the repo listed there.

Okay, now you have the repo URL in your clipboard so it's time to clone! Open up a command prompt on your computer and type `git clone ` and then paste in the URL. Hit enter to get the ball rolling.

You should see something like this:

![screen shot 2016-11-04 at 12 21 55 am](https://cloud.githubusercontent.com/assets/1182323/19995382/b8fea24c-a224-11e6-8e85-443940ab3515.png)

When it finishes, you'll have a clone of your OpenPnP repo on your computer. It will be in a subdirectory called `openpnp`.

Tip: Don't forget where you placed your repo. I like to keep all my repos in a directory called Projects. So on my computer my repo is under `Projects/openpnp`.

# A Brief Interlude

Before we get down to making changes, let's take a little break and read the [OpenPnP Developer's Guide](https://github.com/openpnp/openpnp/wiki/Developers-Guide#building-openpnp). You don't have to read the whole thing right now, but you should definitely read the [Building OpenPnP Section](https://github.com/openpnp/openpnp/wiki/Developers-Guide#building-openpnp).

If you are a Java programmer you probably have your own preferred set of tools. The Developers Guide shows you how to use Eclipse and Maven to work with OpenPnP, but you can use whatever suits you. It won't matter for this tutorial.

For this tutorial, I'm going to make my changes in Eclipse but everything you need to know about Git will still be done at the command line.

With that out of the way, it's time to...

# Make Some Changes!

This is the fun part. This is the reason we're here! It's time to make OpenPnP better.






