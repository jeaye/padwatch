# padwatch

padwatch is a continuous apartment listing watcher which pulls data from
Craigslist and Zillow. It's currently supports:

* Continuous scraping of apartment listings
* Walkscore lookup with geotag based on listing or census.gov lookup
* SQLite3 backend for tracking old listings
* IRC reporting with tinyurl shortening

### Running
```bash
# Straight from lein
$ lein run

# To deploy
$ lein uberjar
$ java -jar target/uberjar/padwatch-0.1.0-SNAPSHOT-standalone.jar
```

### Configuring
A default configuration is provided
[here](https://github.com/jeaye/padwatch/blob/master/resources/config.edn). All
supported properties are contained, so just tweak as you'd like.
