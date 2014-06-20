BackFire
========
BackFire is an officially supported [Backbone](http://backbonejs.org) binding for
[Helium](http://www.helium.com/?utm_medium=web&utm_source=backfire).
The bindings let you use a special Collection type that will automatically
synchronize all models contained within it to Helium, without the need
to make explicit calls to save or sync.

### Live Demo: <a target="_blank" href="http://helium.github.io/backfire">Real-time TODO app</a>.

Usage
-----
Include both helium.js and backbone-helium.js in your application.

```html
<script src="https://cdn.helium.com/v0/helium.js"></script>
<script src="backbone-helium.js"></script>
```

There are two primary ways to use the bindings:

### METHOD 1: Backbone.Helium.Collection

You will have access to a new object, `Backbone.Helium.Collection`. You
may extend this object, and must provide a Helium URL or a Helium reference
as the `helium` property. For example:

```js
var TodoList = Backbone.Helium.Collection.extend({
  model: Todo,
  helium: "https://<your-helium>.heliumio.com"
});
```

You may also apply a `limit` or some other [query](https://www.helium.com/docs/queries.html)
on a reference and pass it in:

```js
var Messages = Backbone.Helium.Collection.extend({
  helium: new Helium("https://<your-helium>.heliumio.com").limit(10)
});
```

Any models added to the collection will be synchronized to the provided
Helium. Any other clients using the Backbone binding will also receive
`add`, `remove` and `changed` events on the collection as appropriate.

**BE AWARE!** The important difference between using a regular collection and
a Helium collection is that you do not need to call any functions that will
affect _remote_ data. If you call `fetch` or `sync` on the collection, **the
library will ignore it silently**.

You should add and remove your models to the collection as your normally would,
(via `add` and `remove`) and _remote_ data will be instantly updated.
Subsequently, the same events will fire on all your other clients immediately.

Please see [todos.js](https://github.com/helium/backfire/blob/gh-pages/todos.js)
for an example of how to use this special collection object.

### METHOD 2: Backbone.sync

The bindings also override `Backbone.sync` to use Helium. You may consider
this option if you want to maintain an explicit seperation between _local_ and
_remote_ data.

This adapter works very similarly to the
[localStorage adapter](http://documentcloud.github.com/backbone/docs/backbone-localstorage.html)
used in the canonical Todos example. You simply provide a `helium` property
in your Model or Collection, and that object will be persisted at that location.

For example:

```js
var TodoList = Backbone.Collection.extend({
  model: Todo,
  helium: new Backbone.Helium("https://<your-namespace>.heliumio.com")
});
```

will ensure that any calls to `fetch` or `sync` on the collection will update
the provided Helium with the appropriate data. The same is true for the
`save` and `destroy` methods on a model.

Please see [todos-sync.js](https://github.com/helium/backfire/blob/gh-pages/todos-sync.js)
for an example of how to use this feature.

License
-------
[MIT](http://helium.mit-license.org).
