define(
    ["homecommon"],
    function( hc ){
        function Home( container ){
            this.container = container;
            this.hc = new hc(container);
            this.hc.init( function( dom ){
              //window.location.href =  $('li a').eq(0).attr('href') ;
            });

            this.init = function(){
                
            }
            
        }

        return Home;
    }
)