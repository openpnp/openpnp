# Read This First
To submit a Pull Request for OpenPnP you must use this template or it will be deleted. 

Be sure to review the [[Developers Guide]] and the [Contributing Guidelines](https://github.com/openpnp/openpnp/blob/develop/CONTRIBUTING.md).

Make your Pull Request as small as possible. Only include one bug or feature.
Large pull requests that change dozens of files or add multiple features are unlikely to be accepted.

Fill out all the details below.

-----------------------------------------------------------------------

# Description
Describe the change in detail. Explain how it works. The better you describe the change the more likely it is to be accepted.

# Justification
Why should this change be included in OpenPnP? Does it benefit a majority of users or is it specific to one type of user or machine?

# Instructions for Use
How does someone use the feature? Be descriptive. Include step by step instructions. If this is a new feature then these instructions will become the Wiki documentation for the feature, so explain here the same way you would explain to someone who had never used the feature.

This is documentation for a user, not for a developer.

# Implementation Details
1. How did you test the change?
2. Did you add automated tests?
3. Did you follow the [coding style](https://github.com/openpnp/openpnp/wiki/Developers-Guide#coding-style)?
4. If you made changes in the `org.openpnp.spi` or `org.openpnp.model` packages you will need to add additional justification for these changes. Changes to these packages require extensive review and testing.
