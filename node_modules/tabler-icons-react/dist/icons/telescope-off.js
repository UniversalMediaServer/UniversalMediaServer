import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function TelescopeOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-telescope-off",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 21.002l6 -5l6 5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 13.002v8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8.238 8.264l-4.183 2.51c-1.02 .614 -1.357 1.898 -.76 2.906l.165 .28c.52 .88 1.624 1.266 2.605 .91l6.457 -2.34m2.907 -1.055l4.878 -1.77a1.023 1.023 0 0 0 .565 -1.455l-2.62 -4.705a1.087 1.087 0 0 0 -1.447 -.42l-.056 .032l-6.016 3.61"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 5.002l3 5.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { TelescopeOff as default };
