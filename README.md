# movielens

Demonstration project using [Titanium](https://github.com/clojurewerkz/titanium) to store
and query the [MovieLens](http://grouplens.org/datasets/movielens/) dataset.

## Usage

This project requires pre-release versions of Archimedes, Titanium, and Ogre:

* Archimedes - build from [humbled-transactions](https://github.com/clojurewerkz/archimedes/tree/humbled-transactions) branch
* Titanium - build from [archimedes-api-change](https://github.com/clojurewerkz/titanium/tree/archimedes-api-change) branch
* Ogre - build from [blueprints-2.5.0](https://github.com/clojurewerkz/ogre/tree/blueprints-2.5.0) branch

The [MovieLens](http://grouplens.org/datasets/movielens/) dataset is not included here, but is freely available for
download. The 100k dataset is ample for this project, and can be downloaded [here](http://files.grouplens.org/datasets/movielens/ml-100k.zip).

Assuming you download and unzip into the resources folder, you can get started via:

    (require '[clojure.java.io :as io])
    (require '[uk.org.1729.movielens :as ml])
    
    (def graph (ml/init-graph "/tmp/movielens"))
    (ml/load-data graph (io/resource "ml-100k"))

## License

Copyright © 2014 Ray Miller <ray@1729.org.uk>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
