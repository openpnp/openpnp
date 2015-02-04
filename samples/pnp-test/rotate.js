var radius = 0.75;
for (var i = 2, angle = 0; i <= 25; i++, angle += 15) {
    console.log('MOVE R' + i + ' (P ' + radius + ' ' + angle + ')');
    console.log('ROTATE =R' + angle + ' \'R' + i + '\'');
}