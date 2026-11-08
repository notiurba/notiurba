async page => {
  const out=[];for(const p of page.context().pages()){out.push({url:p.url(),text:(await p.locator('body').innerText()).slice(0,1400)})};return out;
}
