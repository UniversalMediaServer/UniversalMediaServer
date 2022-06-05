import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Rotate2(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-rotate-2",
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
    d: "M15 4.55a8 8 0 0 0 -6 14.9m0 -4.45v5h-5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "18.37",
    y1: "7.16",
    x2: "18.37",
    y2: "7.17"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "13",
    y1: "19.94",
    x2: "13",
    y2: "19.95"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "16.84",
    y1: "18.37",
    x2: "16.84",
    y2: "18.38"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "19.37",
    y1: "15.1",
    x2: "19.37",
    y2: "15.11"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "19.94",
    y1: "11",
    x2: "19.94",
    y2: "11.01"
  }));
}

export { Rotate2 as default };
