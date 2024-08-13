
import GrooveElements from "./GrooveElements";
import eventReloads from "./eventReloads";
const BoardMethods = {
    finishLoading: () => {
        try {
            $(window).trigger("finishedLoading")
            const loader = document.getElementById("loader")
            setTimeout(() => {
                loader.classList.add("finished")
                setTimeout(() => {
                    loader.remove()
                }, 500);
            }, 500);
        } catch (error) {

        }
    },
    createHomeTile: (size = [1, 1], options = {}) => {
        options = Object.assign({ imageIcon: false, icon: "", title: "Unknown", packageName: "com.unknown", supportedSizes: ["s", "m"] }, options)
        setTimeout(() => {
            scrollers.tile_page_scroller.refresh()

        }, 0);
        console.log("created", options)
        const widget = window.tileListGrid.addWidget(
            GrooveElements.wHomeTile(options.imageIcon, options.icon, options.title, options.packageName, "", options.supportedSizes),
            {
                w: size[0],
                h: size[1],
            })
        if (window.scrollers) window.scrollers.tile_page_scroller.refresh()
        return widget

        options = Object.assign({ imageIcon: false, icon: "", title: "Unknown", packageName: "com.unknown", color: "default" }, options)
        switch (String(size)) {
            case "1,1":

                break;
            case "2,1":
                el.classList.add("")
                break;
            case "2,2":

                break;

            default:
                break;
        }
        const el = GrooveElements.wHomeTile(options.imageIcon, options.icon, options.title, options.packageName, options.color)
        document.querySelector("div.tile-list-page div.tile-list-inner-container").appendChild(el)
        return el
    },
    createAppTile: (options) => {
        options = Object.assign({ imageIcon: false, icon: "", title: "Unknown", packageName: "com.unknown" }, options)
        const el = GrooveElements.wAppTile(options.imageIcon, options.icon, options.title, options.packageName)
        document.querySelector("#main-home-slider > div > div:nth-child(2) > div > div.app-list > div.app-list-container").appendChild(el)

        // window.scrollers.app_page_scroller.refresh()
        return el
    },
    createLetterTile: (letter) => {
        const el = GrooveElements.wLetterTile(letter)
        document.querySelector("#main-home-slider > div > div:nth-child(2) > div > div.app-list > div.app-list-container").appendChild(el)
        return el
    },
    createAppMenu: (packageName) => {
        const el = GrooveElements.wAppMenu(packageName, {
            "pin to start": () => {
                const findTile = $(`div.inner-page.app-list-page > div.app-list > div.app-list-container > div.groove-element.groove-app-tile[packagename="${packageName}"]`)[0]
                console.log("BUNDAN TİLE", findTile)

                GrooveBoard.BoardMethods.createHomeTile([1, 1], {
                    packageName: findTile.getAttribute("packagename"),
                    title: findTile.getAttribute("title"),
                    icon: findTile.getAttribute("icon"),
                    imageIcon: findTile.getAttribute("imageicon") == "true",
                    //  supportedSizes: ["s", "m", "w", "l"]
                    supportedSizes: ["s", "m", "w", "l"]
                })
                scrollers.tile_page_scroller.refresh()
                setTimeout(() => {
                    scrollers.main_home_scroller.scrollTo(0, 0, 500)
                    setTimeout(() => {
                        scrollers.tile_page_scroller.scrollTo(0, scrollers.tile_page_scroller.maxScrollY, 500)

                    }, 300);
                }, 300);
            }, "uninstall": () => {
                Bridge.requestAppUninstall(packageName)
            }
        })
        document.querySelector("div.app-list-page").appendChild(el)
        return el
    },
    createTileMenu: (el) => {
        document.querySelectorAll(".groove-tile-menu").forEach(i => i.remove())
        const tileMenu = GrooveElements.wTileMenu(el)
        el.appendChild(tileMenu)
        return el
    }
}
var appSortCategories = {}
window.appSortCategories = appSortCategories
function getLabelRank(char) {
    if (/^\d+$/.test(char)) {
        return 1; // Numbers
    } else if (/^[A-Za-z]+$/.test(char)) {
        return 2; // Letters (both uppercase and lowercase)
    } else {
        return 3; // Special characters
    }
}
function getLabelSortCategory(label) {
    const firstletter = String(label)[0]
    const labelRank = getLabelRank(window.normalizeDiacritics(String(label)[0]).toLocaleLowerCase("en"))
    if (labelRank == 1) {
        return "0-9"
    } if (labelRank == 2) {
        return window.normalizeDiacritics(firstletter).toLocaleLowerCase("en").toLocaleUpperCase("en");
    } else {
        return "&"
    }
}
function sortObjectsByLabel(a, b) {
    let labelA = window.normalizeDiacritics(String(a.label)).toLocaleLowerCase("en");
    let labelB = window.normalizeDiacritics(String(b.label)).toLocaleLowerCase("en");
    // Get the ranks for the first characters in the labels
    let rankA = getLabelRank(labelA[0]);
    let rankB = getLabelRank(labelB[0]);
    if (rankA === rankB) {
        // If ranks are the same, sort by label case-insensitively
        return labelA.localeCompare(labelB, "en", { sensitivity: "base" });
    } else {
        // Otherwise, sort by rank
        return rankA - rankB;
    }
}
const originalWidgetSizes = [98.5, 209, 319.5430]
function sortObjectsByKey(a, b) {
    let labelA = window.normalizeDiacritics(String(a[0])).toLocaleLowerCase("en");
    let labelB = window.normalizeDiacritics(String(b[0])).toLocaleLowerCase("en");
    // Get the ranks for the first characters in the labels
    let rankA = getLabelRank(labelA[0]);
    let rankB = getLabelRank(labelB[0]);
    if (rankA === rankB) {
        // If ranks are the same, sort by label case-insensitively
        return labelA.localeCompare(labelB, "en", { sensitivity: "base" });
    } else {
        // Otherwise, sort by rank
        return rankA - rankB;
    }
}


// Output: [{ label: "1" }, { label: "2" }, { label: "A" }, { label: "a" }, { label: "B" }, { label: "c" }, { label: "#" }, { label: "@" }]
const BackendMethods = {
    reloadApps: function (callback) {
        fetch(Bridge.getAppsURL())
            .then(resp => resp.json())
            .then(resp => {
                // replaceApps(resp);
                let array = resp.apps
                array.sort(sortObjectsByLabel);
                window["allappsarchive"] = array
                array.forEach(entry => {
                    const labelSortCategory = getLabelSortCategory(entry.label)
                    if (!!!appSortCategories[labelSortCategory]) appSortCategories[labelSortCategory] = []
                    appSortCategories[labelSortCategory].push(entry)

                });
                //appSortCategories = 
                appSortCategories = (Object.fromEntries(Object.entries(appSortCategories).sort(sortObjectsByKey)))
                Object.keys(appSortCategories).forEach(labelSortCategory => {
                    let letter = BoardMethods.createLetterTile(labelSortCategory == "0-9" ? "#" : labelSortCategory == "&" ? "" : labelSortCategory.toLocaleLowerCase("en"))
                    appSortCategories[labelSortCategory].forEach(app => {
                        const ipe = window.iconPackDB[app.packageName]
                        const el = BoardMethods.createAppTile({ title: app.label, packageName: app.packageName, imageIcon: ipe ? false : true, icon: ipe ? ipe.icon : Bridge.getDefaultAppIconURL(app.packageName) })
                        if (ipe) { if (ipe.pack == 0) el.classList.add("iconpack0"); else el.classList.add("iconpack1") }
                    });
                    // BoardMethods.createAppTile({ title: entry.label })
                });
                scrollers.app_page_scroller.refresh()
                eventReloads.appTile()
                /*
              // springBoard.reloadPages()
              if (callback && typeof callback == "function") callback(); else {
                  //  console.log("couldnt call callback")
              }*/
                // $("body").append(new cupertinoElements.appIcon("../mock/icons/default/com.android.chrome.png", "bb", "cc"))
            })
    },
    refreshInsets: () => {
        if (window.stopInsetUpdate) return;
        console.log("tamam düzeltiyorum")
        window.windowInsets = JSON.parse(Groove.getSystemInsets());
        Object.keys(windowInsets).forEach((element) => {
            document.body.style.setProperty("--window-inset-" + element, windowInsets[element] + "px");
        });
    },
    navigation: {
        history: [],
        push: (change, forwardAction, backAction) => {
            GrooveBoard.BackendMethods.navigation.invalidate(change)

            console.log("HISTORY PUSH", change)
            forwardAction()
            BackendMethods.navigation.history.push({ forwardAction: forwardAction, change: change, backAction })
            history.pushState(change, "", window.location.href); // Explicitly using the current URL
            listHistory()
        },
        back: (action = true) => {
            if (BackendMethods.navigation.history.length <= 1) return
            if (action == false) BackendMethods.navigation.history.reverse()[0].backAction = () => { }
            const act = BackendMethods.navigation.history.pop()
            console.log("HISTORY BACK", act.change)
            act.backAction()
            listHistory()
        },
        get lastPush() {
            if (GrooveBoard.BackendMethods.navigation.history.length == 0) return undefined
            return GrooveBoard.BackendMethods.navigation.history.slice(-1)[0]
        },
        invalidate: (change) => {
            if (GrooveBoard.BackendMethods.navigation.history.length == 0) return undefined
            console.log("HISTORY INVA", change)
            if (GrooveBoard.BackendMethods.navigation.lastPush.change == change) {
                GrooveBoard.BackendMethods.navigation.back(false)
            }
            listHistory()
        }
    },
    getTileSize: function (w, h) {
        const padding = 12
        const column = document.querySelector("div.tile-list-inner-container").classList.contains("gs-4") ? 4 : document.querySelector("div.tile-list-inner-container").classList.contains("gs-6") ? 6 : 8
        const base = (document.querySelector("div.tile-list-inner-container").clientWidth / column) - padding
        return [w * base + (w - 1) * padding, h * base + (h - 1) * padding]
    },
    scaleTiles: function () {
        const scale = GrooveBoard.BackendMethods.getTileSize(1, 1)[0] / originalWidgetSizes[0]
        document.querySelector("div.tile-list-inner-container").style.setProperty("--tile-zoom", scale)
    },
    resizeTile: function (el, size) {
        const appSizeDictionary = { s: [1, 1], m: [2, 2], w: [4, 2], l: [4, 4] }
        if (!appSizeDictionary[size] || !el["gridstackNode"]) return
        const chosenSize = appSizeDictionary[size]
        if (size == "s") {
            el.removeAttribute("gs-w")
            el.removeAttribute("gs-h")
        } else {
            el.setAttribute("gs-w", chosenSize[0])
            el.setAttribute("gs-h", chosenSize[1])
        }
        tileListGrid.moveNode(el.gridstackNode, { w: chosenSize[0], h: chosenSize[1] })
    }
}
function listHistory() {
    console.log("%c " + GrooveBoard.BackendMethods.navigation.history.map(e => JSON.stringify(e)).join("\n"), 'background: #222; color: #bada55')
}
window.addEventListener("backButtonPress", function () {
    console.log("back button press")
    BackendMethods.navigation.back()
})
export default { BoardMethods, BackendMethods }

