import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function LayoutOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-layout-off",
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
    d: "M8 4a2 2 0 0 1 2 2m-1.162 2.816a1.993 1.993 0 0 1 -.838 .184h-2a2 2 0 0 1 -2 -2v-1c0 -.549 .221 -1.046 .58 -1.407"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "4",
    y: "13",
    width: "6",
    height: "7",
    rx: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 10v-4a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v10m-.595 3.423a1.994 1.994 0 0 1 -1.405 .577h-2a2 2 0 0 1 -2 -2v-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { LayoutOff as default };
