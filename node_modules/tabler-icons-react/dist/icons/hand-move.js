import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function HandMove(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-hand-move",
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
    d: "M8 13v-8.5a1.5 1.5 0 0 1 3 0v7.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 11.5v-2a1.5 1.5 0 0 1 3 0v2.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 10.5a1.5 1.5 0 0 1 3 0v1.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 11.5a1.5 1.5 0 0 1 3 0v4.5a6 6 0 0 1 -6 6h-2h.208a6 6 0 0 1 -5.012 -2.7l-.196 -.3c-.312 -.479 -1.407 -2.388 -3.286 -5.728a1.5 1.5 0 0 1 .536 -2.022a1.867 1.867 0 0 1 2.28 .28l1.47 1.47"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M2.541 5.594a13.487 13.487 0 0 1 2.46 -1.427"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 3.458c1.32 .354 2.558 .902 3.685 1.612"
  }));
}

export { HandMove as default };
