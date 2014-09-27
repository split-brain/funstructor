'use strict';

var BASE_URL = location.host;
var PORT     = location.port;

var funs = funs || {};

// WEB SOCKET
(function(funs){
    var ws;

    funs.websocket = {
        _ws : new WebSocket("ws://" + BASE_URL + '/ws'),
        _events : {},
        send : function(m){
            if(typeof m !== 'string'){
                m = JSON.stringify(m);
            }
            this._ws.send(m);
            return this;
        },
        on : function(event, callback){
            this._events[event] = this._events[event] || [];
            this._events[event].push(callback);
            return this;
        },
        off : function(event, callback){
            if(this._events[event]){
                if(callback){
                    this._events[event] = this._events[event].filter(function(c){
                        return callback !== c;
                    });
                }else{
                    delete this._events[event];
                }
            }
            return this;
        }
    };
    
    ws = funs.websocket._ws;
    
    ws.onopen = function(event){
        ws.onmessage = function (event) {
            var message = JSON.parse(event.data),
                events  = funs.websocket._events[message.type];
            if(events){
                events.forEach(function(callback){
                    callback(message);
                });
            }
        };
    };
    
})(funs);

// WEBSOCKET ROUTER
(function(funs){
    var ws = funs.websocket;
    
    var ROUTES = {
        'game-request-ok' :  function(data){
            console.log('game-request-ok', data);
        }
    };

    for(var route in ROUTES){
        ws.on(route, ROUTES[route]);
    }
})(funs);


// START
(function(funs){
    // draw cards on a field
    var flyingCards = document.getElementById('flyingCards');
    var CARDS = [
        "mutator_left_gap.svg"
        ,"mutator_pos_gap.svg"
        ,"mutator_right_gap.svg"
        ,"terminal_id.svg"
        ,"terminal_left_paren.svg"
        ,"terminal_left_square.svg"
        ,"terminal_num.svg"
        ,"terminal_right_paren.svg"
        ,"terminal_right_square.svg"
    ];
    
    drawCards();
    
    var $form = $('#startForm');
    $form.on('submit', function(e){
        e.preventDefault();
        funs.websocket.send({type:"game-request"});
    });
    
    function drawCards(){
        var $body = $('body');
        var width  = $body.width();
        var height = $body.height();
        var cards = React.createClass({
            render: function() {
                var R = React.DOM;
                var d = 90;
                var cards = this.props.cards;
                
                console.log('cards',cards);

                return R.div({
                    key: 'cards'
                }, cards.map(function(card, i){
                    var xr = Math.round(Math.random() * d);
                    var yr = Math.round(Math.random() * d);
                    var zr = Math.round(Math.random() * d);
                    
                    var style = {
                        transform: 'rotateX( ' + xr + 'deg ) rotateY( ' + yr + 'deg ) rotateZ( ' + zr + 'deg ) ',
                        left : (200 + Math.random() * (width - 400)) + 'px',
                        top  : (200 + Math.random() * (height - 400)) + 'px'
                    };
                    return funs.Views.Card({
                        url: card,
                        style: style
                    });
                }));
            }
        });
        
        React.renderComponent(cards({
            cards : CARDS
        }), flyingCards);
    }
})(funs);

(function(funs){
})(funs);
