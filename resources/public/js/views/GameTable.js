//            <section id="game">
//                <div class="field opponent"></div>
//                <div class="field your"></div>
//                <div class="hand"></div>
//                <div class="legend"></div>
//            </section>

'use strict';

var funs = funs || {};

funs.Views = funs.Views || {};

funs.Views.GameTable = React.createClass({
    render: function() {
        var R = React.DOM;
        var update = this.props;
        console.log('game-update',update);

        var children = [
            funs.Views.YourField     (null, update.funstruct),
            funs.Views.OpponentField (null, update['enemy-funstruct']),
            funs.Views.YourHand      (null, update.cards),
            funs.Views.OpponentHand  (null, update['enemy-cards-num']),
            funs.Views.Log           (null, update['current-turn']),
            funs.Views.Task          (null, funs.state.gameData.goal)
        ];

        if(!funs.state.myTurn){
            children.push(funs.Views.OpponentTurn());
        }
        setTimeout(function(){
            funs.$body.hide(0).show(0);
        }, 20);
        var game = R.section({
                id: 'game'
            }, children);
        
        return game;
    }
});

funs.Views.OpponentTurn = React.createClass({
    render: function(){
        var R = React.DOM;
        return R.div({
            className: 'opponentTurn'
        },R.div(null, funs.state.enemy + '\'s Turn'));
    }
});

funs.Views.YourField = React.createClass({
    preventDefault: function (event) {
        event.preventDefault();
    },
    drop: function(event){
        event.preventDefault();
        if(!funs.state.myTurn){
            return;
        }
        var data;
        try {
            data = JSON.parse(event.dataTransfer.getData('text/plain'));
        } catch (e) {
            console.warn(e);
            return;
        }
        
        funs.ondrop({
            type: 'YourField',
            drag: data,
            drop: this.props
        });
    },
    render: function() {
        var R = React.DOM;
        var functors = this.props.children;

        var funList = [],
            l = functors.length;
        
        functors.forEach(function(f,i){
            f.i = i;
            f.self = true;
            funList.push(funs.Views.Func(f));
            funList.push(funs.Views.Func({
                terminal : 'space',
                value : null,
                i : i,
                self : true
            }));
        });

        return R.div({
            'onDrop'  : this.drop,
            'onDragOver'    : this.preventDefault,
            className : 'field your'
        },funList);
    }
});
funs.Views.OpponentField = React.createClass({
    render: function() {
        var R = React.DOM;
        var functors = this.props.children;

        var funList = [],
            l = functors.length;
        
        functors.forEach(function(f,i){
            f.i = i;
            funList.push(funs.Views.Func(f));
            if(i < (l - 1) || true){
                funList.push(funs.Views.Func({
                    terminal : 'space',
                    value : null,
                    i : i
                }));
            }
        });

        return R.div({
            className: 'field opponent'
        },funList);
    }
});

funs.Views.YourHand = React.createClass({
    render: function() {
        var R = React.DOM;
        var cardModels = this.props.children;
        var _draggable = true;
        var options = {
                className : 'card'
            };

        if(!funs.state.myTurn){
            _draggable = false;
        }

        var cards = cardModels.map(function(c, i){
            return R.div(options, funs.Views.Card({
                    _draggable : _draggable,
                    style : {},
                    url   : c.img,
                    data : c,
                    index : i
                }));
        });

        return R.div({
            className: 'hand your'
        }, cards);
    }
});
funs.Views.OpponentHand = React.createClass({
    render: function() {
        var R = React.DOM;
        var number = this.props.children;
        var cards = [];

        for (var i = 0, max = number; i < max; i++) {
            cards.push(R.div({
                className: 'card'
            }, funs.Views.Card({
                    style : {},
                    url   : 'back.svg'
                })));
        }

        return R.div({
            className: 'hand opponent'
        }, cards);
    }
});

funs.Views.Log = React.createClass({
    render: function() {
        var R = React.DOM;
        
        var data = {
            logs: funs.state.logs || []
        };

        var children = [
            funs.Views.LogStream(data),
            funs.Views.SendMessage()
        ];

        return R.div({
            className: 'log'
        },children);
    }
});
funs.Views.LogStream = React.createClass({
    render: function(){
        var R = React.DOM;
        var data = this.props.children;
        var logs = this.props.logs;
        console.log('LogStream', this.props);
        var children = logs.map(function(l){
            var message = R.div({
                className: 'message'
            }, JSON.stringify(l));
            return message;
        });
        return R.div({
            className: 'logStream'
        },children);
    }
});
funs.Views.SendMessage = React.createClass({
    keypress: function(e){
        if(e.which === 13) {
            var $input = $(e.currentTarget);
            var action = {
                type : 'chat-message',
                data : {
                    'game-id' : funs.state['game-id'],
                    'message'   : $input.val()
                }
            };
            funs.websocket.send(action);
            $input.val('');
        }
    },
    render: function(){
        var R = React.DOM;
        
        return R.div({
            className: 'sendMessage input'
        }, R.input({
            onKeyPress: this.keypress,
            type: 'text'
        }));
    }
});

funs.Views.Task = React.createClass({
    render: function() {
        var R = React.DOM;

        var children = [
            R.h3(null, this.props.children.name),
            R.p(null, this.props.children.raw),
            funs.Views.EndTurn()
        ];

        return R.div({
            className: 'task'
        }, children);
    }
});

funs.Views.EndTurn = React.createClass({
    click: function(){
        if(!funs.state.myTurn){
            return;
        }
        funs.endTurn();
    },
    render: function() {
        var R = React.DOM;
        return R.div({
            className: 'button endTurn'
        },R.button({
            onClick: this.click
        }, 'End Turn'));
    }
});

funs.Views.Func = React.createClass({

    preventDefault: function (event) {
        event.preventDefault();
    },

    drop: function (event) {
        event.preventDefault();
        if(!funs.state.myTurn){
            return;
        }
        var data;
        try {
            data = JSON.parse(event.dataTransfer.getData('text/plain'));
        } catch (e) {
            console.warn(e);
            return;
        }
        funs.ondrop({
            type: 'Func',
            drag: data,
            drop: this.props
        });
    },
  
    render: function() {
        var R = React.DOM;
        var text;
        var terminal = this.props.terminal;
        console.log('FUNC', this.props);
        var symDict = {
            'gap' : '_',
            'space' : R.img({src : '/img/e.png'}),
            'left-paren' : '(',
            'right-paren' : ')',
            'left-square' : '[',
            'right-square' : ']',
            'id' : this.props.value,
            'num' : this.props.value
        };
        
        text = symDict[terminal] || 'UN';
        
        var options = {
            className       : 'terminal ' + terminal + ' i_' + this.props.i,
            'data-index'    : this.props.i,
            'data-terminal' : terminal
        };
        if(this.props.self){
            options.onDragOver = this.preventDefault;
            options.onDrop = this.drop;
        }
        
        return R.span(options, text);
    }
});


