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
            funs.Views.Task          (null, funs.state['task'])
        ];

        var game = R.section({
                id: 'game'
            }, children);
        
        return game;
    }
});

funs.Views.YourField = React.createClass({
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
            className: 'field your'
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

        var cards = cardModels.map(function(c, i){
            return R.div({
                className : 'card',
                draggable : "true",
                ondragstart : '"funs.ondragstart(event)"'
            }, funs.Views.Card({
                    style : {},
                    url   : c.img
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

        return R.div({
            className: 'log'
        },this.props.children);
    }
});

funs.Views.Task = React.createClass({
    render: function() {
        var R = React.DOM;

        return R.div({
            className: 'task'
        },this.props.children);
    }
});

funs.Views.Func = React.createClass({
    render: function() {
        var R = React.DOM;
        var text;
        var terminal = this.props.terminal;
        console.log('FUNC', this.props);
        
        switch(terminal){
            case 'gap':
                text = '_';
                break;
            case 'space':
                text = ' ';
                break;
            default :
                text = '';
                break;
        }
        
        return R.span({
            className       : 'terminal ' + terminal + ' i_' + this.props.i,
            'data-index'    : this.props.i,
            'data-terminal' : terminal,
            'onDragOver'    : funs.ondragover,
            'onDrop'        : funs.ondrop
        },text);
    }
});


