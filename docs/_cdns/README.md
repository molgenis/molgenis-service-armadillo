# Get rid of CDNs

We need to protect our reseachers and make the docs more performant.

- `mkdir _cdns`
- Check index.html for external links
- fetch all external references

> When adding a new docs tree ie `v3.2.0` make sure to follow these same/similar steps

```bash
wget cdn.jsdelivr.net/npm/docsify@4/lib/themes/vue.css
wget cdn.jsdelivr.net/npm/docsify@4 --output-document=docsify@4.js
wget cdn.jsdelivr.net/npm/docsify-sidebar-collapse/dist/docsify-sidebar-collapse.min.js
wget unpkg.com/docsify/lib/plugins/search.min.js
```

- scan files for external links and change those.
- `vue.css` has link to fonts list

```bash
wget https://fonts.googleapis.com/css\?family\=Roboto+Mono\|Source+Sans+Pro:300,400,600 --output-document=fonts.css
```

- fetch the fonts now listed in `fonts.css`
- fetch those fonts in `./fonts/`

```bash
wget https://fonts.gstatic.com/s/robotomono/v23/L0xuDF4xlVMF-BfR8bXMIhJHg45mwgGEFl0_3vq_ROW9.ttf
wget https://fonts.gstatic.com/s/sourcesanspro/v22/6xKydSBYKcSV-LCoeQqfX1RYOo3ik4zwlxdr.ttf
wget https://fonts.gstatic.com/s/sourcesanspro/v22/6xK3dSBYKcSV-LCoeQqfX1RYOo3qOK7g.ttf
wget https://fonts.gstatic.com/s/sourcesanspro/v22/6xKydSBYKcSV-LCoeQqfX1RYOo3i54rwlxdr.ttf
```

- Adjust the link to the downloaded fonts

```bash
vi fonts.css
# replace all paths ie
# https://fonts.gstatic.com/s/sourcesanspro/v22/xxx.ttf
# into
# ./fonts/xxx.ttf
```
