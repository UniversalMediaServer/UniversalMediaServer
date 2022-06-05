import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function HighlightOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-highlight-off",
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
    d: "M8.998 9.002l-5.998 5.998v4h4l6 -6m1.997 -1.997l2.503 -2.503a2.828 2.828 0 1 0 -4 -4l-2.497 2.497"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12.5 5.5l4 4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4.5 13.5l4 4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 15h2v2m-2 2h-6l3.004 -3.004"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { HighlightOff as default };
