// Copyright: https://github.com/mauriciolobo/todo
const DbStore = require('nedb')
const uuid = require('uuid/v4')
const Vertx = require('@vertx/core').Vertx
const vertx = vertx || new Vertx()

const eventBus = vertx.eventBus()
const db = new DbStore({autoload: true, filename: 'todo'})

async function dbGetAll(message) {
    db.find({}, (err, doc) => {
        message.reply(doc)
    })
}

function dbGetOne(message, _id) {
    db.findOne({_id}, (err, doc) => {
        message.reply(doc)
    })
}

eventBus.consumer("get.todo", message => {
    if (message === 'all') {
        dbGetAll(message)
    } else {
        doGetOne(message, message.body())
    }
})

eventBus.consumer('insert.todo', message => {
    var id = uuid();
    var doc = {
        ...message.body,
        completed: false,
        _id: id,
        id,
        url: req.protocol + '://' + req.get('host') + '/' + id
    };
    db.insert(doc, (err, doc) => {
        message.reply(doc)
    })
})

eventBus.consumer("update.todo", message => {
    db.update({_id: message.body().id}, {$set: message.body()}, {}, (err, number) => {
        dbGetOne(message, message.body().id)
    })
})

eventBus.consumer("delete.todo", message => {
    if (message === 'all') {
        db.remove({}, {multi: true}, (err, n) => {
            dbGetAll(message)
        })
    } else {
        db.remove({_id: message.body()}, {}, (err, n) => {
            dbGetOne(message, message.body())
        })
    }
})
