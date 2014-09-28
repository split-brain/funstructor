'use strict';

var funs = funs || {};

funs.Views = funs.Views || {};

funs.Views.Card = React.createClass({
    dragStart: function (event) {

        var data = {
          data  : this.props.data,
          index : this.props.index 
        };

        event.dataTransfer.setData('text', JSON.stringify(data)); 
        console.log('this.dragStart', data);
    },
    render: function() {
        var R = React.DOM;

        var options = {
                style: this.props.style,
                key: this.props.url
            };

        if(this.props._draggable){
            options.draggable = 'true';
            options.onDragStart = this.dragStart;
        }

        var card = R.div(options, R.img({
                src: '/img/cards/' + this.props.url
            }));
        
        return card;
    }
});
