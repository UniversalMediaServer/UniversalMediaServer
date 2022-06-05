import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function MessageCircleOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-message-circle-off",
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
  }), /*#__PURE__*/React.createElement("line", {
    x1: "3",
    y1: "3",
    x2: "21",
    y2: "21"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8.585 4.581c3.225 -1.181 7.032 -.616 9.66 1.626c2.983 2.543 3.602 6.525 1.634 9.662m-1.908 2.108c-2.786 2.19 -6.89 2.665 -10.271 1.023l-4.7 1l1.3 -3.9c-2.237 -3.308 -1.489 -7.54 1.714 -10.084"
  }));
}

export { MessageCircleOff as default };
