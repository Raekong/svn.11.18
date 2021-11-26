define(
    ["template"],
    function( tpl ){
        function HomeMenu(container){
            this.container = container;
            this.init = function(){
                $.post(
                    '/home/menu',   //取主页侧边的菜单项，每个菜单项包括一个链接和菜单文字
                    function( rst ){
                        console.log(rst);
                    }
                )
            }
        }
        return HomeMenu;
    }
)