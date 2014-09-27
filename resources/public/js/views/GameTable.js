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

        return R.div({
            className: 'field your'
        },this.props.children);
    }
});
funs.Views.OpponentField = React.createClass({
    render: function() {
        var R = React.DOM;

        return R.div({
            className: 'field opponent'
        },this.props.children);
    }
});

funs.Views.YourHand = React.createClass({
    render: function() {
        var R = React.DOM;
        var cardModels = this.props.children;

        var cards = cardModels.map(function(c, i){
            return R.div({
                className: 'card'
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

        return R.div({
            className: 'hand opponent'
        },this.props.children);
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



