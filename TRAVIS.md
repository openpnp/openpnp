# Notes on Travis CI Integration

## Encrypted Variables
* Variables are all encrypted using the travis utility: `gem install travis`.
* env.global.secure: `travis encrypt INSTALL4J_LICENSE_KEY=########################`
* deploy.access_key_id.secure: `travis encrypt #######`
* deploy.secret_access_key.secure: `travis encrypt ########`

## S3
* Installers are uploaded to S3/openpnp and links are on openpnp.org.
* Credentials are for IAM user openpnp-travisci.
