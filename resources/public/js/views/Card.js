'use strict';

var funs = funs || {};

funs.Views = funs.Views || {};

funs.Views.Card = React.createClass({
    render: function() {
        var R = React.DOM;

        var card = R.div({
                style: this.props.style,
                key: this.props.url
            }, R.img({
                src: '/img/cards/' + this.props.url
            }));
        
        return card;
    }
});
