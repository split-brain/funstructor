'use strict';

var funs = funs || {};

funs.Views = funs.Views || {};

funs.Views.Card = React.createClass({
    dragStart: function (event) {

        var data = {
          data  : this.props.data,
          index : this.props.index 
        };
        
        funs.ondragstart(data);
        event.dataTransfer.setData('text/plain', JSON.stringify(data)); 
        
        console.log('DRAG START',event);
        
        var crt = event.currentTarget;

        crt.style.opacity = "0.3";
        event.dataTransfer.setDragImage(crt, 0, 0);
        
    },
    dragEnd: function(event){
        var crt = event.currentTarget;
        crt.style.opacity = "1";
        funs.audio.card_play();
        funs.ondragend();
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
            options.onDragEnd   = this.dragEnd;
        }

        var card = R.div(options, R.img({
                src: '/img/cards/' + this.props.url
            }));
        
        return card;
    }
});
